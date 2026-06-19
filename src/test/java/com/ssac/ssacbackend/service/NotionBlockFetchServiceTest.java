package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.component.NotionImageMigrator;
import com.ssac.ssacbackend.config.NotionProperties;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import notion.api.v1.NotionClient;
import notion.api.v1.model.blocks.BulletedListItemBlock;
import notion.api.v1.model.blocks.Blocks;
import notion.api.v1.model.blocks.BlockType;
import notion.api.v1.model.blocks.NumberedListItemBlock;
import notion.api.v1.model.blocks.QuoteBlock;
import notion.api.v1.model.blocks.TableBlock;
import notion.api.v1.model.blocks.TableRowBlock;
import notion.api.v1.model.blocks.UnsupportedBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotionBlockFetchServiceTest {

    @Mock
    private NotionClient notionClient;
    @Mock
    private NotionProperties notionProperties;
    @Mock
    private NotionImageMigrator notionImageMigrator;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private HttpClient httpClient;

    @InjectMocks
    private NotionBlockFetchService notionBlockFetchService;

    @BeforeEach
    void setUp() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
    }

    @Nested
    @DisplayName("fetchBlocks")
    class FetchBlocks {

        @Test
        @DisplayName("캐시 미스 시 Notion에서 블록을 조회하고 Redis에 캐싱한다")
        void fetchBlocks_캐시미스_Notion조회() {
            Blocks blocks = Mockito.mock(Blocks.class);
            given(blocks.getResults()).willReturn(List.of());
            given(notionClient.retrieveBlockChildren("page-abc", null, 100)).willReturn(blocks);

            List<Map<String, Object>> result = notionBlockFetchService.fetchBlocks("page-abc");

            assertThat(result).isEmpty();
            verify(notionClient).retrieveBlockChildren("page-abc", null, 100);
            verify(valueOperations).set(eq("content:blocks:page-abc"), anyString(), any());
        }

        @Test
        @DisplayName("캐시 히트 시 Notion 호출 없이 캐시된 블록을 반환한다")
        void fetchBlocks_캐시히트_Notion미호출() {
            given(valueOperations.get("content:blocks:page-abc")).willReturn("[]");

            List<Map<String, Object>> result = notionBlockFetchService.fetchBlocks("page-abc");

            assertThat(result).isEmpty();
            verify(notionClient, never()).retrieveBlockChildren(anyString(), any(), any());
        }

        @Test
        @DisplayName("notionPageId가 null이면 빈 리스트를 반환하고 Notion을 호출하지 않는다")
        void fetchBlocks_nullPageId() {
            List<Map<String, Object>> result = notionBlockFetchService.fetchBlocks(null);

            assertThat(result).isEmpty();
            verify(notionClient, never()).retrieveBlockChildren(anyString(), any(), any());
        }

        @Test
        @DisplayName("블록 목록에 null 요소가 포함되어도 빈 리스트를 반환하고 예외가 발생하지 않는다")
        void fetchBlocks_null요소_포함_예외없음() {
            Blocks blocks = Mockito.mock(Blocks.class);
            given(blocks.getResults()).willReturn(java.util.Arrays.asList(null, null));
            given(notionClient.retrieveBlockChildren("page-abc", null, 100)).willReturn(blocks);

            List<Map<String, Object>> result = notionBlockFetchService.fetchBlocks("page-abc");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("hasMore가 true이면 nextCursor로 다음 페이지를 연속 조회한다")
        void fetchBlocks_페이지네이션_연속조회() {
            BulletedListItemBlock block1 = Mockito.mock(BulletedListItemBlock.class);
            given(block1.getType()).willReturn(BlockType.BulletedListItem);
            given(block1.getId()).willReturn("block-1");

            BulletedListItemBlock block2 = Mockito.mock(BulletedListItemBlock.class);
            given(block2.getType()).willReturn(BlockType.BulletedListItem);
            given(block2.getId()).willReturn("block-2");

            Blocks page1 = Mockito.mock(Blocks.class);
            given(page1.getResults()).willReturn(List.of(block1));
            given(page1.getHasMore()).willReturn(Boolean.TRUE);
            given(page1.getNextCursor()).willReturn("cursor-1");

            Blocks page2 = Mockito.mock(Blocks.class);
            given(page2.getResults()).willReturn(List.of(block2));
            given(page2.getHasMore()).willReturn(Boolean.FALSE);

            given(notionClient.retrieveBlockChildren("page-abc", null, 100)).willReturn(page1);
            given(notionClient.retrieveBlockChildren("page-abc", "cursor-1", 100)).willReturn(page2);

            List<Map<String, Object>> result = notionBlockFetchService.fetchBlocks("page-abc");

            assertThat(result).hasSize(2);
            verify(notionClient).retrieveBlockChildren("page-abc", null, 100);
            verify(notionClient).retrieveBlockChildren("page-abc", "cursor-1", 100);
        }

        @Test
        @DisplayName("연속된 numbered_list_item 블록에 1부터 시작하는 number 필드가 주입된다")
        void fetchBlocks_번호매기기목록_순번주입() {
            NumberedListItemBlock.Element element = new NumberedListItemBlock.Element(List.of());
            NumberedListItemBlock block1 = new NumberedListItemBlock(element);
            block1.setId("item-1");
            NumberedListItemBlock block2 = new NumberedListItemBlock(element);
            block2.setId("item-2");
            NumberedListItemBlock block3 = new NumberedListItemBlock(element);
            block3.setId("item-3");

            Blocks blocks = Mockito.mock(Blocks.class);
            given(blocks.getResults()).willReturn(List.of(block1, block2, block3));
            given(blocks.getHasMore()).willReturn(Boolean.FALSE);
            given(notionClient.retrieveBlockChildren("page-abc", null, 100)).willReturn(blocks);

            List<Map<String, Object>> result = notionBlockFetchService.fetchBlocks("page-abc");

            assertThat(result).hasSize(3);
            for (int i = 0; i < 3; i++) {
                assertThat(result.get(i).get("type")).isEqualTo("numbered_list_item");
                @SuppressWarnings("unchecked")
                Map<String, Object> itemProp =
                    (Map<String, Object>) result.get(i).get("numbered_list_item");
                assertThat(itemProp).isNotNull();
                assertThat(itemProp.get("number")).isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("다른 블록 타입이 중간에 등장하면 번호 매기기 목록의 순번이 1로 초기화된다")
        void fetchBlocks_번호매기기목록_순번초기화() {
            NumberedListItemBlock.Element element = new NumberedListItemBlock.Element(List.of());
            NumberedListItemBlock firstBlock = new NumberedListItemBlock(element);
            firstBlock.setId("item-1");

            BulletedListItemBlock separator = Mockito.mock(BulletedListItemBlock.class);
            given(separator.getType()).willReturn(BlockType.BulletedListItem);
            given(separator.getId()).willReturn("bullet-1");

            NumberedListItemBlock afterReset = new NumberedListItemBlock(element);
            afterReset.setId("item-2");

            Blocks blocks = Mockito.mock(Blocks.class);
            given(blocks.getResults()).willReturn(List.of(firstBlock, separator, afterReset));
            given(blocks.getHasMore()).willReturn(Boolean.FALSE);
            given(notionClient.retrieveBlockChildren("page-abc", null, 100)).willReturn(blocks);

            List<Map<String, Object>> result = notionBlockFetchService.fetchBlocks("page-abc");

            assertThat(result).hasSize(3);
            @SuppressWarnings("unchecked")
            Map<String, Object> firstProp =
                (Map<String, Object>) result.get(0).get("numbered_list_item");
            assertThat(firstProp.get("number")).isEqualTo(1);

            @SuppressWarnings("unchecked")
            Map<String, Object> afterResetProp =
                (Map<String, Object>) result.get(2).get("numbered_list_item");
            assertThat(afterResetProp.get("number")).isEqualTo(1);
        }

        @Test
        @DisplayName("SDK가 인식하지 못한 UnsupportedBlock은 Notion API 직접 조회로 실제 타입을 복원한다")
        @SuppressWarnings("unchecked")
        void fetchBlocks_unsupportedBlock_원본타입복원() throws Exception {
            UnsupportedBlock unsupportedBlock = Mockito.mock(UnsupportedBlock.class);
            given(unsupportedBlock.getType()).willReturn(BlockType.Unsupported);
            given(unsupportedBlock.getId()).willReturn("heading4-id");

            Blocks blocks = Mockito.mock(Blocks.class);
            given(blocks.getResults()).willReturn(List.of(unsupportedBlock));
            given(blocks.getHasMore()).willReturn(Boolean.FALSE);
            given(notionClient.retrieveBlockChildren("page-abc", null, 100)).willReturn(blocks);

            String rawJson = "{\"id\":\"heading4-id\",\"type\":\"heading_4\","
                + "\"has_children\":false,"
                + "\"heading_4\":{\"rich_text\":[{\"plain_text\":\"제목4\"}]}}";
            HttpResponse<String> httpResponse = Mockito.mock(HttpResponse.class);
            given(httpResponse.statusCode()).willReturn(200);
            given(httpResponse.body()).willReturn(rawJson);
            given(notionProperties.getApiKey()).willReturn("test-key");
            given(httpClient.send(
                Mockito.any(HttpRequest.class),
                Mockito.<HttpResponse.BodyHandler<String>>any()
            )).willReturn((HttpResponse) httpResponse);

            List<Map<String, Object>> result = notionBlockFetchService.fetchBlocks("page-abc");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("type")).isEqualTo("heading_4");
            assertThat(result.get(0)).containsKey("heading_4");
        }

        @Test
        @DisplayName("table 블록의 자식(table_row)이 children으로 포함된다")
        void fetchBlocks_테이블블록_행포함() {
            TableRowBlock.Element rowElement = new TableRowBlock.Element();
            TableRowBlock tableRowBlock = new TableRowBlock(rowElement);
            tableRowBlock.setId("row-1");

            Blocks rowBlocks = Mockito.mock(Blocks.class);
            given(rowBlocks.getResults()).willReturn(List.of(tableRowBlock));
            given(rowBlocks.getHasMore()).willReturn(Boolean.FALSE);

            TableBlock.Element tableElement = new TableBlock.Element(2, true, false);
            TableBlock tableBlock = new TableBlock(tableElement);
            tableBlock.setId("table-1");
            tableBlock.setHasChildren(true);

            Blocks topBlocks = Mockito.mock(Blocks.class);
            given(topBlocks.getResults()).willReturn(List.of(tableBlock));
            given(topBlocks.getHasMore()).willReturn(Boolean.FALSE);

            given(notionClient.retrieveBlockChildren("page-abc", null, 100)).willReturn(topBlocks);
            given(notionClient.retrieveBlockChildren("table-1", null, 100)).willReturn(rowBlocks);

            List<Map<String, Object>> result = notionBlockFetchService.fetchBlocks("page-abc");

            assertThat(result).hasSize(1);
            Map<String, Object> tableMap = result.get(0);
            assertThat(tableMap.get("type")).isEqualTo("table");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children =
                (List<Map<String, Object>>) tableMap.get("children");
            assertThat(children).hasSize(1);
            assertThat(children.get(0).get("type")).isEqualTo("table_row");
        }

        @Test
        @DisplayName("인용 블록의 자식(글머리, 코드 등)이 재귀적으로 조회된다")
        void fetchBlocks_인용블록_자식_재귀조회() {
            // 글머리 자식 블록 (BulletedListItem)
            BulletedListItemBlock bulletBlock = Mockito.mock(BulletedListItemBlock.class);
            given(bulletBlock.getType()).willReturn(BlockType.BulletedListItem);
            given(bulletBlock.getId()).willReturn("bullet-id");
            Blocks childBlocks = Mockito.mock(Blocks.class);
            given(childBlocks.getResults()).willReturn(List.of(bulletBlock));

            // 인용 블록 (has_children = true)
            QuoteBlock quoteBlock = new QuoteBlock();
            quoteBlock.setId("quote-id");
            quoteBlock.setHasChildren(true);

            Blocks topBlocks = Mockito.mock(Blocks.class);
            given(topBlocks.getResults()).willReturn(List.of(quoteBlock));

            given(notionClient.retrieveBlockChildren("page-abc", null, 100)).willReturn(topBlocks);
            given(notionClient.retrieveBlockChildren("quote-id", null, 100)).willReturn(childBlocks);

            List<Map<String, Object>> result = notionBlockFetchService.fetchBlocks("page-abc");

            assertThat(result).hasSize(1);
            Map<String, Object> quoteMap = result.get(0);
            assertThat(quoteMap).containsKey("children");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) quoteMap.get("children");
            assertThat(children).hasSize(1);
            // BlockType.getValue()로 snake_case 타입이 올바르게 포함되어야 한다
            assertThat(children.get(0).get("type")).isEqualTo("bulleted_list_item");
        }
    }

    @Nested
    @DisplayName("fetchChildBlocks")
    class FetchChildBlocks {

        @Test
        @DisplayName("자식 블록을 조회하여 반환한다")
        void fetchChildBlocks_정상() {
            Blocks childBlocks = Mockito.mock(Blocks.class);
            given(childBlocks.getResults()).willReturn(List.of());
            given(notionClient.retrieveBlockChildren("child-block-id", null, 100))
                .willReturn(childBlocks);

            List<Map<String, Object>> result =
                notionBlockFetchService.fetchChildBlocks("child-block-id");

            assertThat(result).isEmpty();
            verify(notionClient).retrieveBlockChildren("child-block-id", null, 100);
        }

        @Test
        @DisplayName("blockId가 null이면 빈 리스트를 반환하고 Notion을 호출하지 않는다")
        void fetchChildBlocks_nullId() {
            List<Map<String, Object>> result =
                notionBlockFetchService.fetchChildBlocks(null);

            assertThat(result).isEmpty();
            verify(notionClient, never()).retrieveBlockChildren(anyString(), any(), any());
        }

        @Test
        @DisplayName("최대 재귀 깊이(10)를 초과하는 자식 블록 요청은 빈 리스트를 반환한다")
        void fetchChildBlocks_최대깊이_초과_빈리스트_반환() {
            // depth > MAX_BLOCK_DEPTH(10)인 상황을 시뮬레이션:
            // 11단계 깊이의 has_children=true 블록을 중첩 구성한다
            // 실제로는 ReflectionTestUtils로 private 오버로드를 직접 호출한다
            List<Map<String, Object>> result =
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    notionBlockFetchService, "fetchChildBlocks", "deep-block-id", 11);

            assertThat(result).isEmpty();
            verify(notionClient, never()).retrieveBlockChildren(anyString(), any(), any());
        }

        @Test
        @DisplayName("자식 블록이 100개를 초과하면 nextCursor로 다음 페이지를 연속 조회한다")
        void fetchChildBlocks_페이지네이션_연속조회() {
            BulletedListItemBlock block1 = Mockito.mock(BulletedListItemBlock.class);
            given(block1.getType()).willReturn(BlockType.BulletedListItem);
            given(block1.getId()).willReturn("block-1");

            BulletedListItemBlock block2 = Mockito.mock(BulletedListItemBlock.class);
            given(block2.getType()).willReturn(BlockType.BulletedListItem);
            given(block2.getId()).willReturn("block-2");

            Blocks page1 = Mockito.mock(Blocks.class);
            given(page1.getResults()).willReturn(List.of(block1));
            given(page1.getHasMore()).willReturn(Boolean.TRUE);
            given(page1.getNextCursor()).willReturn("cursor-1");

            Blocks page2 = Mockito.mock(Blocks.class);
            given(page2.getResults()).willReturn(List.of(block2));
            given(page2.getHasMore()).willReturn(Boolean.FALSE);

            given(notionClient.retrieveBlockChildren("parent-id", null, 100)).willReturn(page1);
            given(notionClient.retrieveBlockChildren("parent-id", "cursor-1", 100)).willReturn(page2);

            List<Map<String, Object>> result =
                notionBlockFetchService.fetchChildBlocks("parent-id");

            assertThat(result).hasSize(2);
            verify(notionClient).retrieveBlockChildren("parent-id", null, 100);
            verify(notionClient).retrieveBlockChildren("parent-id", "cursor-1", 100);
        }

        @Test
        @DisplayName("자식 블록에 has_children이 true이면 손자 블록도 재귀 조회한다")
        void fetchChildBlocks_재귀조회() {
            // 손자 블록 (BulletedListItem, no children)
            BulletedListItemBlock bulletBlock = Mockito.mock(BulletedListItemBlock.class);
            given(bulletBlock.getType()).willReturn(BlockType.BulletedListItem);
            given(bulletBlock.getId()).willReturn("bullet-id");
            Blocks grandChildBlocks = Mockito.mock(Blocks.class);
            given(grandChildBlocks.getResults()).willReturn(List.of(bulletBlock));

            // 자식 블록 (QuoteBlock, has_children = true)
            QuoteBlock quoteBlock = new QuoteBlock();
            quoteBlock.setId("quote-id");
            quoteBlock.setHasChildren(true);

            Blocks childBlocks = Mockito.mock(Blocks.class);
            given(childBlocks.getResults()).willReturn(List.of(quoteBlock));

            given(notionClient.retrieveBlockChildren("toggle-id", null, 100)).willReturn(childBlocks);
            given(notionClient.retrieveBlockChildren("quote-id", null, 100))
                .willReturn(grandChildBlocks);

            List<Map<String, Object>> result =
                notionBlockFetchService.fetchChildBlocks("toggle-id");

            assertThat(result).hasSize(1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> grandChildren =
                (List<Map<String, Object>>) result.get(0).get("children");
            assertThat(grandChildren).hasSize(1);
            assertThat(grandChildren.get(0).get("type")).isEqualTo("bulleted_list_item");
        }
    }

    @Nested
    @DisplayName("migrateImageUrl")
    class MigrateImageUrl {

        @Test
        @DisplayName("file 타입 Image 블록의 URL을 Cloudinary로 교체하고 expiry_time을 제거한다")
        void migrateImageUrl_file타입() {
            given(notionImageMigrator.migrateIfNeeded("https://s3.example.com/image.png"))
                .willReturn("https://res.cloudinary.com/test/image.png");

            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("url", "https://s3.example.com/image.png");
            fileMap.put("expiry_time", "2026-06-04T10:48:55.847Z");
            Map<String, Object> imageMap = new HashMap<>();
            imageMap.put("type", "file");
            imageMap.put("file", fileMap);
            Map<String, Object> block = new HashMap<>();
            block.put("type", "image");
            block.put("image", imageMap);

            ReflectionTestUtils.invokeMethod(notionBlockFetchService, "migrateImageUrl", block);

            assertThat(fileMap.get("url")).isEqualTo("https://res.cloudinary.com/test/image.png");
            assertThat(fileMap).doesNotContainKey("expiry_time");
        }

        @Test
        @DisplayName("external 타입 Image 블록의 URL을 Cloudinary로 교체한다")
        void migrateImageUrl_external타입() {
            given(notionImageMigrator.migrateIfNeeded("https://external.example.com/image.png"))
                .willReturn("https://res.cloudinary.com/test/external.png");

            Map<String, Object> externalMap = new HashMap<>();
            externalMap.put("url", "https://external.example.com/image.png");
            Map<String, Object> imageMap = new HashMap<>();
            imageMap.put("type", "external");
            imageMap.put("external", externalMap);
            Map<String, Object> block = new HashMap<>();
            block.put("type", "image");
            block.put("image", imageMap);

            ReflectionTestUtils.invokeMethod(notionBlockFetchService, "migrateImageUrl", block);

            assertThat(externalMap.get("url"))
                .isEqualTo("https://res.cloudinary.com/test/external.png");
        }

        @Test
        @DisplayName("image가 아닌 블록은 migrateIfNeeded를 호출하지 않는다")
        void migrateImageUrl_비Image블록_스킵() {
            Map<String, Object> block = new HashMap<>();
            block.put("type", "paragraph");

            ReflectionTestUtils.invokeMethod(notionBlockFetchService, "migrateImageUrl", block);

            verify(notionImageMigrator, never()).migrateIfNeeded(anyString());
        }
    }
}

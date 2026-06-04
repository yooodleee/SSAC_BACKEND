package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.component.NotionImageMigrator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import notion.api.v1.NotionClient;
import notion.api.v1.model.blocks.Blocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private NotionImageMigrator notionImageMigrator;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

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
            Blocks blocks = org.mockito.Mockito.mock(Blocks.class);
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
    }

    @Nested
    @DisplayName("fetchChildBlocks")
    class FetchChildBlocks {

        @Test
        @DisplayName("자식 블록을 조회하여 반환한다")
        void fetchChildBlocks_정상() {
            Blocks childBlocks = org.mockito.Mockito.mock(Blocks.class);
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

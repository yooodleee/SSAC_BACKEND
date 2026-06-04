package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.component.NotionImageMigrator;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentProgress;
import com.ssac.ssacbackend.domain.content.ContentViewHistory;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.ContentCompleteResponse;
import com.ssac.ssacbackend.dto.response.ContentDetailResponse;
import com.ssac.ssacbackend.dto.response.ContentItemDto;
import com.ssac.ssacbackend.dto.response.ContentListResponse;
import com.ssac.ssacbackend.dto.response.LevelUpResult;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.ContentViewHistoryRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ContentProgressRepository contentProgressRepository;
    @Mock
    private LevelUpService levelUpService;
    @Mock
    private HomeCacheEvictService homeCacheEvictService;
    @Mock
    private ContentViewHistoryRepository contentViewHistoryRepository;
    @Mock
    private NotionSyncService notionSyncService;
    @Mock
    private NotionClient notionClient;
    @Mock
    private NotionImageMigrator notionImageMigrator;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ContentService contentService;

    private User mockUser;
    private Content mockContent;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(1L);
        given(mockUser.getEmail()).willReturn("user@test.com");

        mockContent = mock(Content.class);
        given(mockContent.getId()).willReturn(10L);
        given(mockContent.getNotionPageId()).willReturn("page-abc");
        given(mockContent.getTitle()).willReturn("테스트 콘텐츠");
        given(mockContent.isPublished()).willReturn(true);
        given(mockContent.getCategories()).willReturn(List.of("AI"));
        given(mockContent.getDomains()).willReturn(new java.util.LinkedHashSet<>());

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
    }

    @Nested
    @DisplayName("getContents")
    class GetContents {

        @Test
        @DisplayName("비로그인 사용자는 완료 여부 조회 없이 콘텐츠 목록을 반환한다")
        void getContents_비로그인() {
            ContentItemDto item = buildContentItemDto("10", false);
            given(notionSyncService.getPublishedContentItems(any(), any(), any()))
                .willReturn(List.of(item));

            ContentListResponse result = contentService.getContents(null, null, null, null);

            assertThat(result.totalCount()).isEqualTo(1);
            verify(contentProgressRepository, never())
                .findCompletedContentIdsByUserEmail(anyString());
        }

        @Test
        @DisplayName("익명 인증 사용자는 비로그인으로 처리한다")
        void getContents_익명인증() {
            AnonymousAuthenticationToken anonAuth = mock(AnonymousAuthenticationToken.class);
            given(anonAuth.isAuthenticated()).willReturn(true);
            ContentItemDto item = buildContentItemDto("10", false);
            given(notionSyncService.getPublishedContentItems(any(), any(), any()))
                .willReturn(List.of(item));

            ContentListResponse result = contentService.getContents(anonAuth, null, null, null);

            verify(contentProgressRepository, never())
                .findCompletedContentIdsByUserEmail(anyString());
        }

        @Test
        @DisplayName("로그인 사용자는 완료된 콘텐츠에 completed=true를 설정한다")
        void getContents_로그인_완료콘텐츠표시() {
            Authentication auth = mock(Authentication.class);
            given(auth.isAuthenticated()).willReturn(true);
            given(auth.getName()).willReturn("user@test.com");

            ContentItemDto item = buildContentItemDto("10", false);
            given(notionSyncService.getPublishedContentItems(any(), any(), any()))
                .willReturn(List.of(item));
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("user@test.com"))
                .willReturn(List.of(10L));

            ContentListResponse result = contentService.getContents(auth, null, null, null);

            assertThat(result.contents().get(0).completed()).isTrue();
        }

        @Test
        @DisplayName("카테고리 파라미터가 있으면 쉼표로 분리하여 필터에 전달한다")
        void getContents_카테고리필터() {
            Authentication auth = mock(Authentication.class);
            given(auth.isAuthenticated()).willReturn(true);
            given(auth.getName()).willReturn("user@test.com");
            given(notionSyncService.getPublishedContentItems(
                eq(List.of("AI", "CS")), any(), any()))
                .willReturn(List.of());
            given(contentProgressRepository.findCompletedContentIdsByUserEmail(anyString()))
                .willReturn(List.of());

            contentService.getContents(auth, "AI,CS", null, null);

            verify(notionSyncService).getPublishedContentItems(
                eq(List.of("AI", "CS")), any(), any());
        }
    }

    @Nested
    @DisplayName("getContent")
    class GetContent {

        @Test
        @DisplayName("캐시 미스 시 Notion에서 블록을 조회하고 Redis에 캐싱한다")
        void getContent_캐시미스_Notion조회() {
            given(contentRepository.findById(10L)).willReturn(Optional.of(mockContent));
            Blocks blocks = mock(Blocks.class);
            given(blocks.getResults()).willReturn(List.of());
            given(notionClient.retrieveBlockChildren("page-abc", null, 100)).willReturn(blocks);

            ContentDetailResponse result = contentService.getContent(10L, null);

            assertThat(result.id()).isEqualTo("10");
            assertThat(result.title()).isEqualTo("테스트 콘텐츠");
            verify(notionClient).retrieveBlockChildren("page-abc", null, 100);
            verify(valueOperations).set(eq("content:blocks:page-abc"), anyString(), any());
        }

        @Test
        @DisplayName("캐시 히트 시 Notion 호출 없이 캐시된 블록을 반환한다")
        void getContent_캐시히트_Notion미호출() {
            given(contentRepository.findById(10L)).willReturn(Optional.of(mockContent));
            given(valueOperations.get("content:blocks:page-abc")).willReturn("[]");

            ContentDetailResponse result = contentService.getContent(10L, null);

            assertThat(result.blocks()).isEmpty();
            verify(notionClient, never()).retrieveBlockChildren(anyString(), any(), any());
        }

        @Test
        @DisplayName("존재하지 않는 ID면 NotFoundException을 던진다")
        void getContent_없는ID() {
            given(contentRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> contentService.getContent(99L, null))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("비공개 콘텐츠면 NotFoundException을 던진다")
        void getContent_비공개() {
            Content unpublished = mock(Content.class);
            given(unpublished.isPublished()).willReturn(false);
            given(contentRepository.findById(20L)).willReturn(Optional.of(unpublished));

            assertThatThrownBy(() -> contentService.getContent(20L, null))
                .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("fetchChildBlocks")
    class FetchChildBlocks {

        @Test
        @DisplayName("자식 블록을 조회하여 반환한다")
        void fetchChildBlocks_정상() {
            Blocks childBlocks = mock(Blocks.class);
            given(childBlocks.getResults()).willReturn(List.of());
            given(notionClient.retrieveBlockChildren("child-block-id", null, 100))
                .willReturn(childBlocks);

            List<Map<String, Object>> result = org.springframework.test.util.ReflectionTestUtils
                .invokeMethod(contentService, "fetchChildBlocks", "child-block-id");

            assertThat(result).isEmpty();
            verify(notionClient).retrieveBlockChildren("child-block-id", null, 100);
        }

        @Test
        @DisplayName("blockId가 null이면 빈 리스트를 반환하고 Notion을 호출하지 않는다")
        void fetchChildBlocks_nullId() {
            List<Map<String, Object>> result = org.springframework.test.util.ReflectionTestUtils
                .invokeMethod(contentService, "fetchChildBlocks", (Object) null);

            assertThat(result).isEmpty();
            verify(notionClient, never()).retrieveBlockChildren(anyString(), any(), any());
        }
    }

    @Nested
    @DisplayName("migrateImageUrl")
    class MigrateImageUrl {

        @Test
        @DisplayName("file 타입 Image 블록의 URL을 Cloudinary로 교체한다")
        void migrateImageUrl_file타입() {
            given(notionImageMigrator.migrateIfNeeded("https://s3.example.com/image.png"))
                .willReturn("https://res.cloudinary.com/test/image.png");

            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("url", "https://s3.example.com/image.png");
            Map<String, Object> imageMap = new HashMap<>();
            imageMap.put("type", "file");
            imageMap.put("file", fileMap);
            Map<String, Object> block = new HashMap<>();
            block.put("type", "image");
            block.put("image", imageMap);

            ReflectionTestUtils.invokeMethod(contentService, "migrateImageUrl", block);

            assertThat(fileMap.get("url")).isEqualTo("https://res.cloudinary.com/test/image.png");
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

            ReflectionTestUtils.invokeMethod(contentService, "migrateImageUrl", block);

            assertThat(externalMap.get("url"))
                .isEqualTo("https://res.cloudinary.com/test/external.png");
        }

        @Test
        @DisplayName("Image가 아닌 블록은 migrateIfNeeded를 호출하지 않는다")
        void migrateImageUrl_비Image블록_스킵() {
            Map<String, Object> block = new HashMap<>();
            block.put("type", "Paragraph");

            ReflectionTestUtils.invokeMethod(contentService, "migrateImageUrl", block);

            verify(notionImageMigrator, never()).migrateIfNeeded(anyString());
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @BeforeEach
        void setUpComplete() {
            given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(mockUser));
            given(contentRepository.findById(10L)).willReturn(Optional.of(mockContent));
            given(contentProgressRepository.findByContentIdAndUserId(10L, 1L))
                .willReturn(Optional.empty());
            given(contentViewHistoryRepository.findByUserIdOrderByViewedAtDesc(1L))
                .willReturn(List.of());
            given(levelUpService.checkAndApplyLevelUp(eq(mockUser), anyString()))
                .willReturn(LevelUpResult.noLevelUp(UserLevel.SEED, null));
        }

        @Test
        @DisplayName("신규 진행 기록을 생성하고 ContentCompleteResponse를 반환한다")
        void complete_신규진행기록() {
            ContentCompleteResponse result = contentService.complete("user@test.com", 10L);

            assertThat(result.contentId()).isEqualTo("10");
            assertThat(result.isCompleted()).isTrue();
            verify(contentProgressRepository).save(any(ContentProgress.class));
        }

        @Test
        @DisplayName("기존 진행 기록이 있으면 업데이트한다")
        void complete_기존진행기록업데이트() {
            ContentProgress existing = buildContentProgress(1L, "half", 50);
            given(contentProgressRepository.findByContentIdAndUserId(10L, 1L))
                .willReturn(Optional.of(existing));

            contentService.complete("user@test.com", 10L);

            assertThat(existing.getProgressRate()).isEqualTo(ContentProgress.COMPLETION_THRESHOLD);
            verify(contentProgressRepository, never()).save(any());
        }

        @Test
        @DisplayName("레벨업이 발생하면 응답에 leveledUp=true를 포함한다")
        void complete_레벨업발생() {
            given(levelUpService.checkAndApplyLevelUp(eq(mockUser), anyString()))
                .willReturn(LevelUpResult.levelUp(UserLevel.SEED, UserLevel.SPROUT));

            ContentCompleteResponse result = contentService.complete("user@test.com", 10L);

            assertThat(result.isLevelUp()).isTrue();
            assertThat(result.previousLevel()).isEqualTo("SEED");
            assertThat(result.newLevel()).isEqualTo("SPROUT");
        }

        @Test
        @DisplayName("완료 후 홈 캐시를 무효화한다")
        void complete_홈캐시무효화() {
            contentService.complete("user@test.com", 10L);

            verify(homeCacheEvictService).evict(1L);
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 NotFoundException을 던진다")
        void complete_없는사용자() {
            given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> contentService.complete("unknown@test.com", 10L))
                .isInstanceOf(NotFoundException.class);
        }
    }

    @Test
    @DisplayName("recordView - 콘텐츠 조회 이력을 저장한다")
    void recordView_정상() {
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(mockUser));

        contentService.recordView("user@test.com", 10L);

        verify(contentViewHistoryRepository).save(any(ContentViewHistory.class));
    }

    @Test
    @DisplayName("recordView - 존재하지 않는 사용자면 NotFoundException을 던진다")
    void recordView_없는사용자() {
        given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.recordView("unknown@test.com", 10L))
            .isInstanceOf(NotFoundException.class);
    }

    private ContentItemDto buildContentItemDto(String id, boolean completed) {
        return new ContentItemDto(id, "콘텐츠 제목", null,
            List.of("AI"), List.of(), "SEED", "왕초보", completed, null);
    }

    private ContentProgress buildContentProgress(Long id, String lastPosition, int progressRate) {
        ContentProgress cp = ContentProgress.builder()
            .user(mockUser)
            .title("테스트")
            .lastPosition(lastPosition)
            .progressRate(progressRate)
            .contentId(10L)
            .build();
        ReflectionTestUtils.setField(cp, "id", id);
        return cp;
    }
}

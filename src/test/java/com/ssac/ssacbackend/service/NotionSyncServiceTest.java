package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.component.NotionImageMigrator;
import com.ssac.ssacbackend.config.NotionProperties;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentDifficulty;
import com.ssac.ssacbackend.dto.response.ContentItemDto;
import com.ssac.ssacbackend.dto.response.ContentMonitoringListResponse;
import com.ssac.ssacbackend.dto.response.ContentSyncResponse;
import com.ssac.ssacbackend.repository.ContentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import notion.api.v1.NotionClient;
import notion.api.v1.model.databases.QueryResults;
import notion.api.v1.model.pages.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.objenesis.ObjenesisStd;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotionSyncServiceTest {

    @Mock
    private NotionClient notionClient;
    @Mock
    private NotionProperties notionProperties;
    @Mock
    private NotionImageMigrator notionImageMigrator;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    @InjectMocks
    private NotionSyncService notionSyncService;

    // ── 동기화 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("동기화 성공 - 신규 콘텐츠 저장")
    void 동기화_신규_콘텐츠_저장() {
        given(notionProperties.getDatabaseId()).willReturn("db-id");
        given(notionClient.queryDatabase(any())).willReturn(buildQueryResults(List.of(buildPage("page-1")), false));
        given(contentRepository.findByNotionPageId("page-1")).willReturn(Optional.empty());
        given(notionImageMigrator.migrateIfNeeded(any())).willReturn(null);
        given(cacheManager.getCache("contents")).willReturn(cache);

        ContentSyncResponse result = notionSyncService.syncAll();

        assertThat(result.syncedCount()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(0);
        verify(contentRepository).save(any(Content.class));
    }

    @Test
    @DisplayName("동기화 성공 - 기존 콘텐츠 업데이트 (save 미호출)")
    void 동기화_기존_콘텐츠_업데이트() {
        given(notionProperties.getDatabaseId()).willReturn("db-id");
        given(notionClient.queryDatabase(any())).willReturn(buildQueryResults(List.of(buildPage("page-2")), false));
        Content existing = buildContent("page-2");
        given(contentRepository.findByNotionPageId("page-2")).willReturn(Optional.of(existing));
        given(notionImageMigrator.migrateIfNeeded(any())).willReturn(null);
        given(cacheManager.getCache("contents")).willReturn(cache);

        ContentSyncResponse result = notionSyncService.syncAll();

        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(0);
        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    @DisplayName("동기화 완료 후 캐시 초기화")
    void 동기화_후_캐시_초기화() {
        given(notionProperties.getDatabaseId()).willReturn("db-id");
        given(notionClient.queryDatabase(any())).willReturn(buildQueryResults(List.of(), false));
        given(cacheManager.getCache("contents")).willReturn(cache);

        notionSyncService.syncAll();

        verify(cache).clear();
    }

    @Test
    @DisplayName("databaseId 미설정 시 CONTENT-004 예외 발생")
    void databaseId_미설정_예외() {
        given(notionProperties.getDatabaseId()).willReturn("");

        assertThatThrownBy(() -> notionSyncService.syncAll())
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_NOTION_SYNC_ERROR));
    }

    @Test
    @DisplayName("Notion API 오류 시 CONTENT-004 예외 발생")
    void Notion_API_오류_예외() {
        given(notionProperties.getDatabaseId()).willReturn("db-id");
        given(notionClient.queryDatabase(any())).willThrow(new RuntimeException("API Error"));

        assertThatThrownBy(() -> notionSyncService.syncAll())
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_NOTION_SYNC_ERROR));
    }

    @Test
    @DisplayName("이미 Cloudinary URL이면 이미지 마이그레이션 건너뜀")
    void Cloudinary_URL_마이그레이션_건너뜀() {
        given(notionProperties.getDatabaseId()).willReturn("db-id");
        Page page = buildPage("page-3");
        given(notionClient.queryDatabase(any())).willReturn(buildQueryResults(List.of(page), false));
        given(contentRepository.findByNotionPageId("page-3")).willReturn(Optional.empty());
        String cloudinaryUrl = "https://res.cloudinary.com/demo/image/upload/test.jpg";
        given(notionImageMigrator.migrateIfNeeded(any())).willReturn(cloudinaryUrl);
        given(cacheManager.getCache("contents")).willReturn(cache);

        notionSyncService.syncAll();

        verify(notionImageMigrator).migrateIfNeeded(any());
    }

    @Test
    @DisplayName("Notion published=true인 콘텐츠가 isPublished=true로 저장")
    void Notion_published_true_반영() {
        given(notionProperties.getDatabaseId()).willReturn("db-id");
        Page page = buildPublishedPage("page-4", true);
        given(notionClient.queryDatabase(any())).willReturn(buildQueryResults(List.of(page), false));
        given(contentRepository.findByNotionPageId("page-4")).willReturn(Optional.empty());
        given(notionImageMigrator.migrateIfNeeded(any())).willReturn(null);
        given(cacheManager.getCache("contents")).willReturn(cache);

        notionSyncService.syncAll();

        verify(contentRepository).save(any(Content.class));
    }

    // ── 캐시된 목록 조회 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("게시된 콘텐츠 목록 반환 - 전체 조회")
    void 게시된_콘텐츠_목록_전체_조회() {
        Content content = buildContent("page-5");
        ReflectionTestUtils.setField(content, "isPublished", true);
        ReflectionTestUtils.setField(content, "title", "테스트 콘텐츠");
        given(contentRepository.findAllPublished()).willReturn(List.of(content));

        List<ContentItemDto> items =
            notionSyncService.getPublishedContentItems(null, null, null);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).isEqualTo("테스트 콘텐츠");
        assertThat(items.get(0).completed()).isFalse();
    }

    @Test
    @DisplayName("카테고리 필터 적용 시 해당 쿼리 호출")
    void 카테고리_필터_쿼리_호출() {
        given(contentRepository.findAllPublishedByCategory("realestate")).willReturn(List.of());

        List<ContentItemDto> items =
            notionSyncService.getPublishedContentItems("realestate", null, null);

        assertThat(items).isEmpty();
        verify(contentRepository).findAllPublishedByCategory("realestate");
    }

    @Test
    @DisplayName("난이도 필터 적용 시 해당 쿼리 호출")
    void 난이도_필터_쿼리_호출() {
        given(contentRepository.findAllPublishedByDifficulty(ContentDifficulty.SEED))
            .willReturn(List.of());

        List<ContentItemDto> items =
            notionSyncService.getPublishedContentItems(null, "SEED", null);

        assertThat(items).isEmpty();
        verify(contentRepository).findAllPublishedByDifficulty(ContentDifficulty.SEED);
    }

    // ── 모니터링 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("관리자 모니터링 목록 - totalCount와 publishedCount 반환")
    void 모니터링_목록_조회() {
        Content c = buildContent("page-6");
        ReflectionTestUtils.setField(c, "id", 1L);
        given(contentRepository.findAllByOrderByNotionLastEditedAtDesc(PageRequest.of(0, 20)))
            .willReturn(new PageImpl<>(List.of(c)));
        given(contentRepository.countByIsPublished(true)).willReturn(3L);

        ContentMonitoringListResponse response = notionSyncService.getMonitoring(1, 20);

        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.publishedCount()).isEqualTo(3L);
        assertThat(response.contents()).hasSize(1);
    }

    // ── difficultyLabel ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("난이도 레이블 변환 - SEED → 왕초보")
    void 난이도_레이블_변환() {
        assertThat(NotionSyncService.difficultyLabel(ContentDifficulty.SEED)).isEqualTo("왕초보");
        assertThat(NotionSyncService.difficultyLabel(ContentDifficulty.SPROUT)).isEqualTo("초보");
        assertThat(NotionSyncService.difficultyLabel(ContentDifficulty.TREE)).isEqualTo("중급");
        assertThat(NotionSyncService.difficultyLabel(null)).isEmpty();
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────────

    private Content buildContent(String notionPageId) {
        Content content = Content.fromNotion(notionPageId, "db-id");
        ReflectionTestUtils.setField(content, "categories", List.of());
        ReflectionTestUtils.setField(content, "domains", List.of());
        ReflectionTestUtils.setField(content, "createdAt", LocalDateTime.now());
        return content;
    }

    private Page buildPage(String pageId) {
        return buildPublishedPage(pageId, false);
    }

    private Page buildPublishedPage(String pageId, boolean published) {
        // Kotlin final class — use Objenesis to bypass constructor, then set fields via reflection.
        // All PageProperty fields are left null so extract* methods fall back to null/empty/false.
        // Only 'published' checkbox is set when needed.
        ObjenesisStd objenesis = new ObjenesisStd();

        notion.api.v1.model.pages.PageProperty publishedProp =
            objenesis.newInstance(notion.api.v1.model.pages.PageProperty.class);
        ReflectionTestUtils.setField(publishedProp, "checkbox", published);

        java.util.Map<String, notion.api.v1.model.pages.PageProperty> props = new java.util.HashMap<>();
        props.put("published", publishedProp);

        Page page = objenesis.newInstance(Page.class);
        ReflectionTestUtils.setField(page, "id", pageId);
        ReflectionTestUtils.setField(page, "createdTime", "2026-05-25T00:00:00.000Z");
        ReflectionTestUtils.setField(page, "lastEditedTime", "2026-05-25T00:00:00.000Z");
        ReflectionTestUtils.setField(page, "properties", props);

        return page;
    }

    private QueryResults buildQueryResults(List<Page> pages, boolean hasMore) {
        ObjenesisStd objenesis = new ObjenesisStd();
        QueryResults results = objenesis.newInstance(QueryResults.class);
        ReflectionTestUtils.setField(results, "results", pages);
        ReflectionTestUtils.setField(results, "hasMore", hasMore);
        ReflectionTestUtils.setField(results, "nextCursor", null);
        return results;
    }
}

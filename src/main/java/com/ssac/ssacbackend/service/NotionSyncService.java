package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.component.NotionImageMigrator;
import com.ssac.ssacbackend.config.NotionProperties;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentCategory;
import com.ssac.ssacbackend.domain.content.ContentDifficulty;
import com.ssac.ssacbackend.dto.response.ContentItemDto;
import com.ssac.ssacbackend.dto.response.ContentMonitoringListResponse;
import com.ssac.ssacbackend.dto.response.ContentMonitoringListResponse.ContentMonitoringSummary;
import com.ssac.ssacbackend.dto.response.ContentSyncResponse;
import com.ssac.ssacbackend.repository.ContentRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.databases.QueryResults;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.databases.DatabaseProperty;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.databases.QueryDatabaseRequest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notion 데이터베이스와 콘텐츠를 동기화하는 서비스.
 *
 * <p>1시간마다 자동 동기화하며, 관리자가 수동으로 트리거할 수도 있다.
 * 동기화 완료 시 콘텐츠 목록 캐시를 초기화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotionSyncService {

    private static final String CACHE_CONTENTS = "contents:v2";

    private final NotionClient notionClient;
    private final NotionProperties notionProperties;
    private final NotionImageMigrator notionImageMigrator;
    private final ContentRepository contentRepository;
    private final CacheManager cacheManager;

    // ── 자동 동기화 ────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 3600000)
    public void scheduledSync() {
        log.info("Notion 자동 동기화 시작");
        try {
            ContentSyncResponse result = syncAll();
            log.info("Notion 자동 동기화 완료: synced={}, created={}, updated={}",
                result.syncedCount(), result.createdCount(), result.updatedCount());
        } catch (Exception e) {
            log.error("Notion 자동 동기화 실패", e);
        }
    }

    // ── 수동 동기화 ────────────────────────────────────────────────────────────

    /**
     * Notion 데이터베이스를 조회하여 콘텐츠를 동기화한다.
     *
     * @return 동기화 결과
     */
    @Transactional
    public ContentSyncResponse syncAll() {
        if (notionProperties.getDatabaseId() == null || notionProperties.getDatabaseId().isBlank()) {
            throw new BadRequestException(ErrorCode.CONTENT_NOTION_SYNC_ERROR);
        }

        List<Page> pages = fetchAllPages();
        int created = 0;
        int updated = 0;

        for (Page page : pages) {
            boolean isNew = upsertContent(page);
            if (isNew) {
                created++;
            } else {
                updated++;
            }
        }

        evictContentsCache();
        log.info("동기화 완료: total={}, created={}, updated={}", pages.size(), created, updated);
        return new ContentSyncResponse(pages.size(), created, updated, LocalDateTime.now());
    }

    // ── 캐시된 콘텐츠 목록 ────────────────────────────────────────────────────

    /**
     * 게시된 콘텐츠 목록을 반환한다. 결과는 Redis에 1시간 캐싱된다.
     *
     * @param category  카테고리 필터 (null 허용)
     * @param difficulty 난이도 필터 (null 허용)
     * @param domain    도메인 필터 (null 허용)
     * @return 콘텐츠 항목 목록 (completed=false)
     */
    @Cacheable(
        cacheNames = CACHE_CONTENTS,
        key = "'list:' + (#categories != null ? #categories.toString() : 'null') + ':' + (#difficulty ?: 'null') + ':' + (#domain ?: 'null')"
    )
    @Transactional(readOnly = true)
    public List<ContentItemDto> getPublishedContentItems(
        List<String> categories, String difficulty, String domain) {

        List<Content> contents = fetchPublished(categories, difficulty, domain);
        return contents.stream()
            .map(c -> new ContentItemDto(
                String.valueOf(c.getId()),
                c.getTitle(),
                c.getThumbnailUrl(),
                List.copyOf(c.getCategories()),
                List.copyOf(c.getDomains()),
                c.getDifficulty() != null ? c.getDifficulty().name() : null,
                difficultyLabel(c.getDifficulty()),
                false,
                c.getPublishedAt()
            ))
            .toList();
    }

    // ── 관리자 모니터링 ────────────────────────────────────────────────────────

    /**
     * 관리자용 콘텐츠 모니터링 목록을 반환한다.
     *
     * @param page 1-based 페이지 번호
     * @param size 페이지 크기
     * @return 모니터링 응답
     */
    @Transactional(readOnly = true)
    public ContentMonitoringListResponse getMonitoring(int page, int size) {
        org.springframework.data.domain.Page<Content> pageResult =
            contentRepository.findAllByOrderByNotionLastEditedAtDesc(
                PageRequest.of(page - 1, size));

        long totalCount = pageResult.getTotalElements();
        long publishedCount = contentRepository.countByIsPublished(true);

        LocalDateTime lastSyncedAt = pageResult.getContent().stream()
            .map(Content::getSyncedAt)
            .filter(t -> t != null)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        List<ContentMonitoringSummary> summaries = pageResult.getContent().stream()
            .map(c -> new ContentMonitoringSummary(
                String.valueOf(c.getId()),
                c.getNotionPageId(),
                c.getTitle(),
                c.isPublished(),
                c.getDifficulty() != null ? c.getDifficulty().name() : null,
                List.copyOf(c.getCategories()),
                List.copyOf(c.getDomains()),
                c.getSyncedAt(),
                c.getNotionLastEditedAt()
            ))
            .toList();

        return new ContentMonitoringListResponse(totalCount, publishedCount, lastSyncedAt, summaries);
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    private List<Page> fetchAllPages() {
        List<Page> all = new ArrayList<>();
        String startCursor = null;
        boolean hasMore = true;

        while (hasMore) {
            QueryDatabaseRequest req =
                new QueryDatabaseRequest(notionProperties.getDatabaseId());
            if (startCursor != null) {
                req.setStartCursor(startCursor);
            }
            QueryResults results;
            try {
                results = notionClient.queryDatabase(req);
            } catch (Exception e) {
                log.error("Notion 데이터베이스 조회 실패: {}", e.getMessage());
                throw new BadRequestException(ErrorCode.CONTENT_NOTION_SYNC_ERROR);
            }
            all.addAll(results.getResults());
            hasMore = Boolean.TRUE.equals(results.getHasMore());
            startCursor = results.getNextCursor();
        }
        return all;
    }

    /**
     * Notion 페이지를 DB에 Upsert한다.
     *
     * @return true이면 신규 생성, false이면 업데이트
     */
    private boolean upsertContent(Page page) {
        String notionPageId = page.getId();
        Optional<Content> existing = contentRepository.findByNotionPageId(notionPageId);

        Content content = existing.orElseGet(() ->
            Content.fromNotion(notionPageId, notionProperties.getDatabaseId()));

        boolean isNew = existing.isEmpty();

        String title = extractTitle(page);
        String rawThumbnail = extractUrl(page, "thumbnail");
        String thumbnailUrl = notionImageMigrator.migrateIfNeeded(rawThumbnail);
        List<String> categories = parseCategories(page);
        List<String> domains = extractMultiSelect(page, "domains");
        ContentDifficulty difficulty = extractDifficulty(page);
        boolean isPublished = extractCheckbox(page, "published");
        LocalDateTime notionCreatedAt = parseDateTime(page.getCreatedTime());
        LocalDateTime notionLastEditedAt = parseDateTime(page.getLastEditedTime());
        LocalDateTime publishedAt = extractDate(page, "publishedAt");

        content.syncFromNotion(title, thumbnailUrl, categories, domains, difficulty,
            isPublished, notionCreatedAt, notionLastEditedAt, publishedAt);

        if (isNew) {
            contentRepository.save(content);
        }
        return isNew;
    }

    private String extractTitle(Page page) {
        PageProperty prop = page.getProperties().get("Name");
        if (prop == null || prop.getTitle() == null || prop.getTitle().isEmpty()) {
            return null;
        }
        return prop.getTitle().stream()
            .map(PageProperty.RichText::getPlainText)
            .reduce("", String::concat);
    }

    private String extractUrl(Page page, String propertyName) {
        PageProperty prop = page.getProperties().get(propertyName);
        if (prop == null) {
            return null;
        }
        return prop.getUrl();
    }

    private List<String> parseCategories(Page page) {
        PageProperty prop = page.getProperties().get("categories");
        if (prop == null || prop.getMultiSelect() == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (DatabaseProperty.MultiSelect.Option option : prop.getMultiSelect()) {
            String tag = option.getName();
            try {
                ContentCategory.fromNotionTag(tag);
                result.add(tag);
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 Notion 카테고리 태그 무시: {}", tag);
            }
        }
        return result;
    }

    private List<String> extractMultiSelect(Page page, String propertyName) {
        PageProperty prop = page.getProperties().get(propertyName);
        if (prop == null || prop.getMultiSelect() == null) {
            return List.of();
        }
        return prop.getMultiSelect().stream()
            .map(DatabaseProperty.MultiSelect.Option::getName)
            .toList();
    }

    private ContentDifficulty extractDifficulty(Page page) {
        PageProperty prop = page.getProperties().get("difficulty");
        if (prop == null || prop.getSelect() == null || prop.getSelect().getName() == null) {
            return null;
        }
        try {
            return ContentDifficulty.valueOf(prop.getSelect().getName().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDateTime extractDate(Page page, String propertyName) {
        PageProperty prop = page.getProperties().get(propertyName);
        if (prop == null || prop.getDate() == null || prop.getDate().getStart() == null) {
            return null;
        }
        return parseDateTime(prop.getDate().getStart());
    }

    private boolean extractCheckbox(Page page, String propertyName) {
        PageProperty prop = page.getProperties().get(propertyName);
        if (prop == null || prop.getCheckbox() == null) {
            return false;
        }
        return prop.getCheckbox();
    }

    private LocalDateTime parseDateTime(String isoString) {
        if (isoString == null || isoString.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(isoString).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private List<Content> fetchPublished(List<String> categories, String difficulty, String domain) {
        ContentDifficulty diffEnum = parseDifficulty(difficulty);
        boolean hasCategory = categories != null && !categories.isEmpty();

        if (hasCategory && categories.size() == 1 && diffEnum != null) {
            return contentRepository.findAllPublishedByCategoryAndDifficulty(categories.get(0), diffEnum);
        }
        if (hasCategory && categories.size() == 1) {
            return contentRepository.findAllPublishedByCategory(categories.get(0));
        }
        if (hasCategory) {
            return contentRepository.findAllPublishedByCategoriesIn(categories);
        }
        if (diffEnum != null) {
            return contentRepository.findAllPublishedByDifficulty(diffEnum);
        }
        if (domain != null) {
            return contentRepository.findAllPublishedByDomain(domain);
        }
        return contentRepository.findAllPublished();
    }

    private ContentDifficulty parseDifficulty(String difficulty) {
        if (difficulty == null) {
            return null;
        }
        try {
            return ContentDifficulty.valueOf(difficulty.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void evictContentsCache() {
        Cache cache = cacheManager.getCache(CACHE_CONTENTS);
        if (cache != null) {
            cache.clear();
            log.debug("콘텐츠 캐시 초기화 완료");
        }
    }

    static String difficultyLabel(ContentDifficulty difficulty) {
        if (difficulty == null) {
            return "";
        }
        return switch (difficulty) {
            case SEED -> "왕초보";
            case SPROUT -> "초보";
            case TREE -> "중급";
        };
    }
}

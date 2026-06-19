package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.dto.response.ContentDetailResponse;
import com.ssac.ssacbackend.dto.response.ContentItemDto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Notion 콘텐츠 목록 조회와 상세 조회를 위임하는 파사드.
 *
 * <p>{@link ContentService}에서 Notion 인프라 관심사({@link NotionSyncService},
 * {@link NotionBlockFetchService})를 분리하여 의존성을 단순화한다.
 */
@Service
@RequiredArgsConstructor
public class NotionContentLoader {

    private final NotionSyncService notionSyncService;
    private final NotionBlockFetchService notionBlockFetchService;

    /**
     * 게시된 콘텐츠 목록을 Notion 캐시에서 반환한다.
     *
     * @param categories 카테고리 필터 (null 허용)
     * @param difficulty 난이도 필터 (null 허용)
     * @param domain     도메인 필터 (null 허용)
     * @return 콘텐츠 목록 DTO
     */
    public List<ContentItemDto> getPublishedContentItems(
        List<String> categories, String difficulty, String domain) {
        return notionSyncService.getPublishedContentItems(categories, difficulty, domain);
    }

    /**
     * 콘텐츠 상세를 조립하여 반환한다.
     *
     * <p>Notion 블록을 조회하고 {@link ContentDetailResponse}로 조립한다.
     *
     * @param content 조회 대상 콘텐츠 엔티티
     * @return 콘텐츠 상세 응답 DTO
     */
    public ContentDetailResponse buildContentDetail(Content content) {
        List<Map<String, Object>> blocks =
            notionBlockFetchService.fetchBlocks(content.getNotionPageId());

        return new ContentDetailResponse(
            String.valueOf(content.getId()),
            content.getNotionPageId(),
            content.getTitle(),
            content.getThumbnailUrl(),
            List.copyOf(content.getCategories()),
            List.copyOf(content.getDomains()),
            content.getDifficulty() != null ? content.getDifficulty().name() : null,
            NotionSyncService.difficultyLabel(content.getDifficulty()),
            content.getNotionCreatedAt(),
            content.getNotionLastEditedAt(),
            blocks
        );
    }
}

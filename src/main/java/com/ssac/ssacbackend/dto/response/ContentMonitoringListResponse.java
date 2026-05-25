package com.ssac.ssacbackend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 콘텐츠 모니터링 목록 응답 DTO.
 */
public record ContentMonitoringListResponse(
    long totalCount,
    long publishedCount,
    LocalDateTime lastSyncedAt,
    List<ContentMonitoringSummary> contents
) {

    /**
     * 콘텐츠 모니터링 요약 항목.
     */
    public record ContentMonitoringSummary(
        String id,
        String notionPageId,
        String title,
        boolean isPublished,
        String difficulty,
        List<String> categories,
        List<String> domains,
        LocalDateTime syncedAt,
        LocalDateTime notionLastEditedAt
    ) {}
}

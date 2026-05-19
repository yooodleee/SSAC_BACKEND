package com.ssac.ssacbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내가 본 콘텐츠 목록 응답 DTO.
 */
public record ViewedContentsResponse(
    int totalCount,
    List<ViewedContentDto> contents
) {

    /**
     * 개별 조회 콘텐츠 항목 DTO.
     */
    public record ViewedContentDto(
        String id,
        String title,
        String category,
        String categoryEmoji,
        String difficulty,
        LocalDateTime viewedAt,
        @JsonProperty("isCompleted") boolean isCompleted
    ) {}
}

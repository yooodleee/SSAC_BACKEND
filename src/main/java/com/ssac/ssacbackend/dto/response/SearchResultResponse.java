package com.ssac.ssacbackend.dto.response;

import java.util.List;

/**
 * 콘텐츠 검색 결과 응답 DTO.
 */
public record SearchResultResponse(
    String query,
    long totalCount,
    int page,
    int size,
    List<SearchItem> results
) {

    public record SearchItem(
        String id,
        String title,
        String category,
        String categoryEmoji,
        String difficulty,
        String difficultyLabel,
        int estimatedMinutes,
        String highlightedTitle
    ) {}
}

package com.ssac.ssacbackend.dto.response;

import java.util.List;

/**
 * 검색어 자동완성 응답 DTO.
 */
public record SearchSuggestionResponse(
    String query,
    List<SuggestionItem> suggestions,
    int totalCount
) {

    public record SuggestionItem(
        String id,
        String keyword,
        String category,
        String categoryEmoji,
        long popularity
    ) {}
}

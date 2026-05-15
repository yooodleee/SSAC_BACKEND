package com.ssac.ssacbackend.dto.response;

import java.util.List;

/**
 * 레벨/카테고리 필터링 콘텐츠 목록 응답 DTO.
 */
public record ContentListResponse(
    String level,
    String category,
    int totalCount,
    List<ContentItemDto> contents
) {}

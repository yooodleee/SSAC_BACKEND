package com.ssac.ssacbackend.dto.response;

import java.util.List;

/**
 * 콘텐츠 목록 응답 DTO.
 */
public record ContentListResponse(
    long totalCount,
    List<ContentItemDto> contents
) {}

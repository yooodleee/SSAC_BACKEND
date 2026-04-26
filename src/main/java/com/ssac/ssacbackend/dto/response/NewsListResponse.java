package com.ssac.ssacbackend.dto.response;

import java.util.List;

/**
 * 뉴스 목록 응답 DTO.
 */
public record NewsListResponse(
    long totalCount,
    boolean hasNext,
    List<NewsItemResponse> contents
) {}

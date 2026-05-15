package com.ssac.ssacbackend.dto.response;

/**
 * 추천 도메인 항목 DTO.
 */
public record RecommendedDomainDto(
    String id,
    String name,
    String emoji,
    String reason
) {}

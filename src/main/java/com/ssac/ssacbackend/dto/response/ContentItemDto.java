package com.ssac.ssacbackend.dto.response;

/**
 * 콘텐츠 목록 항목 DTO.
 */
public record ContentItemDto(
    String id,
    String title,
    String difficulty,
    String difficultyLabel,
    int estimatedMinutes,
    boolean isCompleted,
    long viewCount
) {}

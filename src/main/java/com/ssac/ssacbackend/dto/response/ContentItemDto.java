package com.ssac.ssacbackend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 콘텐츠 목록 항목 DTO.
 */
public record ContentItemDto(
    String id,
    String title,
    String thumbnailUrl,
    List<String> categories,
    List<String> domains,
    String difficulty,
    String difficultyLabel,
    boolean completed,
    LocalDateTime publishedAt
) {}

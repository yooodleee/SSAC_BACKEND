package com.ssac.ssacbackend.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 콘텐츠 상세 응답 DTO.
 *
 * <p>Notion 블록을 실시간으로 포함한다.
 */
public record ContentDetailResponse(
    String id,
    String notionPageId,
    String title,
    String thumbnailUrl,
    List<String> categories,
    List<String> domains,
    String difficulty,
    String difficultyLabel,
    LocalDateTime notionCreatedAt,
    LocalDateTime notionLastEditedAt,
    List<Map<String, Object>> blocks
) {}

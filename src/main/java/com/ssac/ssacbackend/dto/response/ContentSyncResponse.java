package com.ssac.ssacbackend.dto.response;

import java.time.LocalDateTime;

/**
 * 콘텐츠 Notion 동기화 결과 응답 DTO.
 */
public record ContentSyncResponse(
    int syncedCount,
    int createdCount,
    int updatedCount,
    LocalDateTime syncedAt
) {}

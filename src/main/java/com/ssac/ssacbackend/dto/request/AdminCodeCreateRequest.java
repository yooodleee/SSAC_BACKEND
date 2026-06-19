package com.ssac.ssacbackend.dto.request;

import java.time.OffsetDateTime;

/**
 * 관리자 코드 발급 요청 DTO.
 *
 * <p>adminUserId: 코드와 연결할 관리자 사용자 ID
 * <p>expiresAt: 만료 일시 (null이면 무기한). 타임존 오프셋을 반드시 포함해야 한다.
 *              예: 2026-06-17T23:00:00+09:00 (KST) 또는 2026-06-17T14:00:00Z (UTC)
 *              내부적으로 KST(Asia/Seoul) LocalDateTime으로 변환하여 저장한다.
 */
public record AdminCodeCreateRequest(
    Long adminUserId,
    OffsetDateTime expiresAt
) {
}

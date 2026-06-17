package com.ssac.ssacbackend.dto.request;

import java.time.LocalDateTime;

/**
 * 관리자 코드 발급 요청 DTO.
 *
 * <p>adminUserId: 코드와 연결할 관리자 사용자 ID
 * <p>expiresAt: 만료 일시 (null이면 무기한). KST(Asia/Seoul) 기준으로 입력해야 한다.
 *              예: 2026-06-17T23:00:00
 */
public record AdminCodeCreateRequest(
    Long adminUserId,
    LocalDateTime expiresAt
) {
}

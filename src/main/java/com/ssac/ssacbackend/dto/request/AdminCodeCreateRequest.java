package com.ssac.ssacbackend.dto.request;

import java.time.LocalDateTime;

/**
 * 관리자 코드 발급 요청 DTO.
 *
 * <p>adminUserId: 코드와 연결할 관리자 사용자 ID
 * <p>expiresAt: 만료 일시 (null이면 무기한)
 */
public record AdminCodeCreateRequest(
    Long adminUserId,
    LocalDateTime expiresAt
) {
}

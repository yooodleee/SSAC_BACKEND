package com.ssac.ssacbackend.dto.response;

import java.time.LocalDateTime;

/**
 * 관리자 코드 발급 응답 DTO.
 *
 * <p>rawCode는 이 응답에서 단 한 번만 노출된다.
 * DB에는 SHA-256 해시만 저장되므로 원문을 분실하면 재발급이 필요하다.
 */
public record AdminCodeCreateResponse(
    String codeId,
    String rawCode,
    Long adminUserId,
    LocalDateTime expiresAt,
    LocalDateTime createdAt
) {
}

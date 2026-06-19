package com.ssac.ssacbackend.dto.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 관리자 코드 발급 응답 DTO.
 *
 * <p>rawCode는 이 응답에서 단 한 번만 노출된다.
 * DB에는 SHA-256 해시만 저장되므로 원문을 분실하면 재발급이 필요하다.
 * expiresAt, createdAt은 KST(+09:00) OffsetDateTime으로 반환한다.
 */
public record AdminCodeCreateResponse(
    String codeId,
    String rawCode,
    Long adminUserId,
    OffsetDateTime expiresAt,
    OffsetDateTime createdAt
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static AdminCodeCreateResponse of(
        String codeId,
        String rawCode,
        Long adminUserId,
        LocalDateTime expiresAtKst,
        LocalDateTime createdAtKst
    ) {
        return new AdminCodeCreateResponse(
            codeId,
            rawCode,
            adminUserId,
            expiresAtKst != null ? expiresAtKst.atZone(KST).toOffsetDateTime() : null,
            createdAtKst != null ? createdAtKst.atZone(KST).toOffsetDateTime() : null
        );
    }
}

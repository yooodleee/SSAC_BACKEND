package com.ssac.ssacbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 이메일 중복 확인 응답 DTO (GET /api/v1/auth/email/check).
 */
public record EmailCheckResponse(
    @JsonProperty("isAvailable") boolean isAvailable
) {
    public static EmailCheckResponse available() {
        return new EmailCheckResponse(true);
    }

    public static EmailCheckResponse unavailable() {
        return new EmailCheckResponse(false);
    }
}

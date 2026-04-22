package com.ssac.ssacbackend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 성공 응답 DTO.
 *
 * <p>Access Token은 응답 바디에 포함되며, Refresh Token은 HttpOnly 쿠키로만 전달된다.
 */
public record LoginResponse(

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    String accessToken,

    @Schema(description = "토큰 타입", example = "Bearer")
    String tokenType

) {
}

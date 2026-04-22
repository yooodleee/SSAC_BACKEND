package com.ssac.ssacbackend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 성공 응답 DTO.
 *
 * <p>발급된 JWT 액세스 토큰을 포함한다.
 */
public record LoginResponse(

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    String token

) {
}

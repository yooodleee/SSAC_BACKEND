package com.ssac.ssacbackend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;

/**
 * 일회용 인가 코드(authCode) 교환 결과 응답 DTO.
 *
 * <ul>
 *   <li>기존 회원: {@code isNewUser=false}, {@code accessToken}/{@code refreshToken} 채워짐</li>
 *   <li>신규 회원: {@code isNewUser=true}, {@code tempToken}/{@code provider} 채워짐</li>
 * </ul>
 */
public record AuthTokenResponse(

    @Schema(description = "신규 회원 여부")
    boolean isNewUser,

    @Nullable
    @Schema(description = "JWT 액세스 토큰 (기존 회원 전용)", example = "eyJhbGciOiJIUzI1NiJ9...")
    String accessToken,

    @Nullable
    @Schema(description = "JWT 리프레시 토큰 (기존 회원 전용)", example = "dGhpcyBpcyBhIHJlZnJlc2g...")
    String refreshToken,

    @Nullable
    @Schema(description = "토큰 타입 (기존 회원 전용)", example = "Bearer")
    String tokenType,

    @Nullable
    @Schema(description = "임시 토큰 (신규 회원 전용 — 회원 가입 플로우에 사용)", example = "550e8400-e29b-41d4-a716-446655440000")
    String tempToken,

    @Nullable
    @Schema(description = "OAuth 공급자 (신규 회원 전용)", example = "NAVER")
    String provider

) {

    public static AuthTokenResponse existingUser(String accessToken, String refreshToken) {
        return new AuthTokenResponse(false, accessToken, refreshToken, "Bearer", null, null);
    }

    public static AuthTokenResponse newUser(String tempToken, String provider) {
        return new AuthTokenResponse(true, null, null, null, tempToken, provider);
    }
}

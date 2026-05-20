package com.ssac.ssacbackend.dto.response;

/**
 * 관리자 코드 로그인 성공 응답 DTO.
 */
public record AdminLoginResponse(
    String accessToken,
    String tokenType,
    long accessTokenExpiresIn,
    UserInfo user
) {
    public record UserInfo(
        String id,
        String nickname,
        String role,
        String redirectTo
    ) {
    }
}

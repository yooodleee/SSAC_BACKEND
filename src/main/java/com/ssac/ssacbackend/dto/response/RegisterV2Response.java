package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;

/**
 * 신규 회원 가입 완료 응답 DTO (POST /api/v1/auth/register).
 */
public record RegisterV2Response(
    String accessToken,
    String tokenType,
    long accessTokenExpiresIn,
    UserInfo user
) {
    public record UserInfo(
        String id,
        String nickname,
        String name,
        String email,
        UserType userType,
        UserLevel level,
        boolean onboardingCompleted
    ) {}
}

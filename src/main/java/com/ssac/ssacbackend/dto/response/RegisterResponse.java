package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;

/**
 * 회원 가입 완료 응답 DTO.
 */
public record RegisterResponse(
    String accessToken,
    String refreshToken,
    UserInfo user,
    MergedInfo merged
) {
    public record UserInfo(
        Long id,
        String nickname,
        UserType userType,
        UserLevel level,
        boolean onboardingCompleted
    ) {}

    public record MergedInfo(int quizCount) {}
}

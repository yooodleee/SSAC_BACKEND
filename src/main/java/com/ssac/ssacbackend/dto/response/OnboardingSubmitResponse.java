package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.user.UserLevel;

/**
 * 온보딩 테스트 제출 완료 응답 DTO.
 */
public record OnboardingSubmitResponse(
    UserLevel level,
    int totalScore,
    boolean onboardingCompleted
) {}

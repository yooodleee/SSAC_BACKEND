package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.user.UserLevel;

/**
 * 온보딩 건너뛰기 응답 DTO.
 */
public record OnboardingSkipResponse(
    UserLevel level,
    boolean onboardingCompleted,
    boolean skipped
) {}

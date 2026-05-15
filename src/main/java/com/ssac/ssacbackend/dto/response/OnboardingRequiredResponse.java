package com.ssac.ssacbackend.dto.response;

/**
 * 온보딩 미완료 사용자에게 반환하는 리다이렉트 응답.
 */
public record OnboardingRequiredResponse(
    boolean onboardingRequired,
    String redirectTo
) {}

package com.ssac.ssacbackend.dto.response;

import java.util.List;

/**
 * 온보딩 레벨 판정 결과 조회 응답 DTO.
 */
public record OnboardingResultResponse(
    String userType,
    String level,
    int totalScore,
    int maxScore,
    String levelLabel,
    String levelEmoji,
    String levelDescription,
    boolean skipped,
    boolean onboardingCompleted,
    List<RecommendedDomainDto> recommendedDomains
) {}

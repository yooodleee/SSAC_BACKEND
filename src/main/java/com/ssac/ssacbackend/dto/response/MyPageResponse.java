package com.ssac.ssacbackend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 마이페이지 프로필 응답 DTO.
 */
public record MyPageResponse(
    String id,
    String email,
    String nickname,
    String userType,
    String userTypeLabel,
    String level,
    String levelLabel,
    String levelEmoji,
    boolean onboardingCompleted,
    List<String> interests,
    StatsDto stats,
    String provider,
    LocalDateTime createdAt
) {

    /**
     * 학습 통계 DTO.
     */
    public record StatsDto(
        long totalContentsCompleted,
        long totalQuizCompleted,
        int correctRate,
        int continuousLearningDays
    ) {
    }
}

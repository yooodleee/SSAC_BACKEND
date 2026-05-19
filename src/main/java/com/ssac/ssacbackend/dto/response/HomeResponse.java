package com.ssac.ssacbackend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 홈 화면 응답 DTO.
 *
 * <p>온보딩 완료 사용자의 맞춤 홈 화면 데이터를 담는다.
 * 온보딩 미완료 시 {@link OnboardingRequiredResponse}를 대신 반환한다.
 */
public record HomeResponse(
    boolean onboardingRequired,
    HomeUserDto user,
    TodayCardDto todayCard,
    List<RecommendedContentDto> recommendedContents,
    ContinueLearningDto continueLearning,
    TodayQuizDto todayQuiz,
    List<CategoryDto> categories,
    LastVisitDto lastVisit,
    WelcomeBackDto welcomeBack
) {

    public record HomeUserDto(
        String nickname,
        String userType,
        String level,
        String levelLabel,
        String levelEmoji,
        String levelImageKey
    ) {}

    public record TodayCardDto(
        String id,
        String title,
        String category,
        String categoryEmoji,
        int estimatedMinutes
    ) {}

    public record RecommendedContentDto(
        String id,
        String title,
        String category,
        String categoryEmoji,
        String difficultyLabel,
        int estimatedMinutes,
        boolean isCompleted,
        boolean isPreview
    ) {}

    public record ContinueLearningDto(
        String id,
        String title,
        String category,
        int progressRate
    ) {}

    public record TodayQuizDto(
        String id,
        String question,
        String category,
        String difficulty
    ) {}

    public record CategoryDto(
        String id,
        String name,
        String emoji,
        long totalCount,
        long completedCount
    ) {}

    public record LastVisitDto(
        LocalDateTime lastVisitedAt,
        int daysSinceLastVisit
    ) {}

    /**
     * 장기 미접속 복귀 정보. 7일 미만 미접속 시 null.
     */
    public record WelcomeBackDto(
        boolean isLongAbsence,
        int daysSinceLastVisit
    ) {}
}

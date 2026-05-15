package com.ssac.ssacbackend.dto.response;

import java.util.List;

/**
 * 홈 화면 응답 DTO.
 */
public record HomeResponse(
    HomeUserDto user,
    TodayCardDto todayCard,
    List<RecommendedContentDto> recommendedContents,
    ContinueLearningDto continueLearning,
    TodayQuizDto todayQuiz,
    List<CategoryDto> categories
) {

    public record HomeUserDto(
        String nickname,
        String userType,
        String level,
        String levelLabel,
        String levelEmoji
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
        boolean isCompleted
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
}

package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import java.time.LocalDateTime;

/**
 * 퀴즈 제출 응답 DTO.
 *
 * <p>기존 응시 기록 필드에 레벨업 판정 결과를 포함한다.
 */
public record QuizSubmitResponse(

    Long id,
    Long quizId,
    String quizTitle,
    int earnedScore,
    int maxScore,
    int correctCount,
    int totalQuestions,
    double accuracyRate,
    LocalDateTime attemptedAt,

    boolean isLevelUp,
    String currentLevel,
    String previousLevel,
    String newLevel,
    LevelUpProgressDto levelUpProgress

) {

    /**
     * QuizAttempt + 레벨업 결과로 응답 DTO를 생성한다.
     */
    public static QuizSubmitResponse from(QuizAttempt attempt, LevelUpResult levelUpResult) {
        int totalQ = attempt.getQuiz().getTotalQuestions();
        double accuracy = totalQ > 0
            ? Math.round((double) attempt.getCorrectCount() / totalQ * 1000.0) / 10.0
            : 0.0;

        return new QuizSubmitResponse(
            attempt.getId(),
            attempt.getQuiz().getId(),
            attempt.getQuiz().getTitle(),
            attempt.getEarnedScore(),
            attempt.getQuiz().getMaxScore(),
            attempt.getCorrectCount(),
            totalQ,
            accuracy,
            attempt.getAttemptedAt(),
            levelUpResult.leveledUp(),
            levelUpResult.currentLevel() != null ? levelUpResult.currentLevel().name() : null,
            levelUpResult.previousLevel() != null ? levelUpResult.previousLevel().name() : null,
            levelUpResult.newLevel() != null ? levelUpResult.newLevel().name() : null,
            levelUpResult.progress()
        );
    }
}

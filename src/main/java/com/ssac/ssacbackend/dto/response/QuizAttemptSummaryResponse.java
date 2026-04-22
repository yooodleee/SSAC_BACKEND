package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 퀴즈 응시 기록 목록 조회 응답 DTO.
 *
 * <p>페이지네이션 리스트에서 한 항목을 나타낸다.
 */
public record QuizAttemptSummaryResponse(

    @Schema(description = "응시 기록 ID", example = "1")
    Long id,

    @Schema(description = "퀴즈 ID", example = "1")
    Long quizId,

    @Schema(description = "퀴즈 제목", example = "Spring Boot 기초")
    String quizTitle,

    @Schema(description = "획득 점수", example = "80")
    int earnedScore,

    @Schema(description = "최고 점수", example = "100")
    int maxScore,

    @Schema(description = "정답 문항 수", example = "8")
    int correctCount,

    @Schema(description = "전체 문항 수", example = "10")
    int totalQuestions,

    @Schema(description = "정답률 (%)", example = "80.0")
    double accuracyRate,

    @Schema(description = "응시 일시")
    LocalDateTime attemptedAt

) {

    /**
     * {@link QuizAttempt} 엔티티로부터 응답 DTO를 생성한다.
     *
     * <p>quiz가 JOIN FETCH 되어 있어야 한다 (LazyInitializationException 방지).
     */
    public static QuizAttemptSummaryResponse from(QuizAttempt attempt) {
        int totalQ = attempt.getQuiz().getTotalQuestions();
        double accuracy = totalQ > 0
            ? Math.round((double) attempt.getCorrectCount() / totalQ * 1000.0) / 10.0
            : 0.0;

        return new QuizAttemptSummaryResponse(
            attempt.getId(),
            attempt.getQuiz().getId(),
            attempt.getQuiz().getTitle(),
            attempt.getEarnedScore(),
            attempt.getQuiz().getMaxScore(),
            attempt.getCorrectCount(),
            totalQ,
            accuracy,
            attempt.getAttemptedAt()
        );
    }
}

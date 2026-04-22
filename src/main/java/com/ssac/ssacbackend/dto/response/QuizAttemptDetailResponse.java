package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.quiz.AttemptAnswer;
import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 퀴즈 응시 상세 조회 응답 DTO.
 *
 * <p>응시 결과와 문항별 답안·정답·획득 점수를 모두 포함한다.
 */
public record QuizAttemptDetailResponse(

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
    LocalDateTime attemptedAt,

    @Schema(description = "문항별 답안 상세")
    List<AnswerDetail> answers

) {

    /**
     * 문항 하나에 대한 답안 상세.
     */
    public record AnswerDetail(

        @Schema(description = "문항 ID", example = "1")
        Long questionId,

        @Schema(description = "문항 내용", example = "Spring Boot의 자동 설정 어노테이션은?")
        String questionContent,

        @Schema(description = "사용자가 선택한 답", example = "A")
        String selectedAnswer,

        @Schema(description = "정답", example = "B")
        String correctAnswer,

        @Schema(description = "정답 여부", example = "false")
        boolean correct,

        @Schema(description = "획득 점수", example = "0")
        int earnedPoints,

        @Schema(description = "이 문항의 배점", example = "10")
        int maxPoints

    ) {

        /**
         * {@link AttemptAnswer}로부터 답안 상세 DTO를 생성한다.
         *
         * <p>question이 JOIN FETCH 되어 있어야 한다.
         */
        public static AnswerDetail from(AttemptAnswer answer) {
            return new AnswerDetail(
                answer.getQuestion().getId(),
                answer.getQuestion().getContent(),
                answer.getSelectedAnswer(),
                answer.getQuestion().getCorrectAnswer(),
                answer.isCorrect(),
                answer.getEarnedPoints(),
                answer.getQuestion().getPoints()
            );
        }
    }

    /**
     * {@link QuizAttempt} 엔티티로부터 상세 응답 DTO를 생성한다.
     *
     * <p>quiz, answers, answers.question이 JOIN FETCH 되어 있어야 한다.
     */
    public static QuizAttemptDetailResponse from(QuizAttempt attempt) {
        int totalQ = attempt.getQuiz().getTotalQuestions();
        double accuracy = totalQ > 0
            ? Math.round((double) attempt.getCorrectCount() / totalQ * 1000.0) / 10.0
            : 0.0;

        List<AnswerDetail> answerDetails = attempt.getAnswers().stream()
            .map(AnswerDetail::from)
            .toList();

        return new QuizAttemptDetailResponse(
            attempt.getId(),
            attempt.getQuiz().getId(),
            attempt.getQuiz().getTitle(),
            attempt.getEarnedScore(),
            attempt.getQuiz().getMaxScore(),
            attempt.getCorrectCount(),
            totalQ,
            accuracy,
            attempt.getAttemptedAt(),
            answerDetails
        );
    }
}

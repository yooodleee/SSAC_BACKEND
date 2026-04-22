package com.ssac.ssacbackend.domain.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 퀴즈 응시 내 문항별 답안 엔티티.
 *
 * <p>한 응시({@link QuizAttempt})에서 각 문항에 제출한 답과 채점 결과를 기록한다.
 *
 * <p>인덱스 전략:
 * - quiz_attempt_id: 응시 상세 조회 시 답안 목록 조회
 */
@Entity
@Table(
    name = "attempt_answers",
    indexes = @Index(name = "idx_attempt_answers_quiz_attempt_id", columnList = "quiz_attempt_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_attempt_id", nullable = false)
    private QuizAttempt quizAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /**
     * 사용자가 제출한 답안 식별자.
     */
    @Column(nullable = false, length = 200)
    private String selectedAnswer;

    /**
     * 정답 여부.
     */
    @Column(nullable = false)
    private boolean correct;

    /**
     * 이 문항에서 획득한 점수 (오답이면 0).
     */
    @Column(nullable = false)
    private int earnedPoints;

    @Builder
    public AttemptAnswer(QuizAttempt quizAttempt, Question question,
        String selectedAnswer, boolean correct, int earnedPoints) {
        this.quizAttempt = quizAttempt;
        this.question = question;
        this.selectedAnswer = selectedAnswer;
        this.correct = correct;
        this.earnedPoints = earnedPoints;
    }
}

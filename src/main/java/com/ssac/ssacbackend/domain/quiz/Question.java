package com.ssac.ssacbackend.domain.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 퀴즈 문항 도메인 엔티티.
 *
 * <p>정답 검증은 서버에서 수행한다. correctAnswer는 클라이언트에 노출하지 않는다.
 */
@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 정답 식별자. 클라이언트가 제출하는 selectedAnswer와 비교한다.
     */
    @Column(nullable = false, length = 200)
    private String correctAnswer;

    /**
     * 이 문항을 맞혔을 때 획득하는 점수.
     */
    @Column(nullable = false)
    private int points;

    /**
     * 퀴즈 내 문항 순서 (1부터 시작).
     */
    @Column(nullable = false)
    private int questionOrder;

    @Builder
    public Question(Quiz quiz, String content, String correctAnswer,
        int points, int questionOrder) {
        this.quiz = quiz;
        this.content = content;
        this.correctAnswer = correctAnswer;
        this.points = points;
        this.questionOrder = questionOrder;
    }
}

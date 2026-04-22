package com.ssac.ssacbackend.domain.quiz;

import com.ssac.ssacbackend.domain.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 퀴즈 응시 기록 엔티티.
 *
 * <p>한 번의 퀴즈 응시를 나타낸다. 문항별 답안은 {@link AttemptAnswer}에 저장된다.
 * earnedScore와 correctCount는 조회 성능을 위해 집계 값을 비정규화하여 저장한다.
 *
 * <p>인덱스 전략:
 * - user_id: 사용자별 기록 조회 (주요 접근 패턴)
 * - attempted_at: 최신 순 정렬 및 기간 필터링
 */
@Entity
@Table(
    name = "quiz_attempts",
    indexes = {
        @Index(name = "idx_quiz_attempts_user_id", columnList = "user_id"),
        @Index(name = "idx_quiz_attempts_attempted_at", columnList = "attempted_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /**
     * 이번 응시에서 획득한 점수.
     */
    @Column(nullable = false)
    private int earnedScore;

    /**
     * 이번 응시에서 맞힌 문항 수.
     */
    @Column(nullable = false)
    private int correctCount;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    @OneToMany(mappedBy = "quizAttempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AttemptAnswer> answers = new ArrayList<>();

    @Builder
    public QuizAttempt(User user, Quiz quiz, int earnedScore, int correctCount) {
        this.user = user;
        this.quiz = quiz;
        this.earnedScore = earnedScore;
        this.correctCount = correctCount;
    }

    /**
     * 문항별 답안을 추가한다.
     */
    public void addAnswer(AttemptAnswer answer) {
        this.answers.add(answer);
    }

    @PrePersist
    private void prePersist() {
        this.attemptedAt = LocalDateTime.now();
    }
}

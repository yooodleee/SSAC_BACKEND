package com.ssac.ssacbackend.domain.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 퀴즈 도메인 엔티티.
 *
 * <p>퀴즈의 메타 정보를 담는다. 문항은 {@link Question}에서 관리한다.
 * maxScore와 totalQuestions는 성능을 위해 비정규화하여 저장한다.
 */
@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    /**
     * 퀴즈 최고 점수 (모든 문항 점수의 합).
     */
    @Column(nullable = false)
    private int maxScore;

    /**
     * 전체 문항 수. 정답률 계산 시 분모로 사용한다.
     */
    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Quiz(String title, String description, int maxScore, int totalQuestions) {
        this.title = title;
        this.description = description;
        this.maxScore = maxScore;
        this.totalQuestions = totalQuestions;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

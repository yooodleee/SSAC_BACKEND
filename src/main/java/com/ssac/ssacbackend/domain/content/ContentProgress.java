package com.ssac.ssacbackend.domain.content;

import com.ssac.ssacbackend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 콘텐츠 학습 진행 상황 엔티티.
 *
 * <p>이어보기 및 세그먼트 산정에 사용된다.
 */
@Entity
@Table(
    name = "content_progress",
    indexes = {
        @Index(name = "idx_content_progress_user_id", columnList = "user_id"),
        @Index(name = "idx_content_progress_updated_at", columnList = "updated_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentProgress {

    /**
     * 학습 완료로 간주하는 progressRate 기준값.
     */
    public static final int COMPLETION_THRESHOLD = 100;

    /**
     * beginner/advanced 세그먼트 분류 기준: 완료 콘텐츠 수.
     */
    public static final int ADVANCED_THRESHOLD = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "last_position", nullable = false, length = 100)
    private String lastPosition;

    /**
     * 학습 진행률 (0~100).
     */
    @Column(name = "progress_rate", nullable = false)
    private int progressRate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ContentProgress(User user, String title, String lastPosition, int progressRate) {
        this.user = user;
        this.title = title;
        this.lastPosition = lastPosition;
        this.progressRate = progressRate;
    }

    /**
     * 학습 위치와 진행률을 갱신한다.
     */
    public void updateProgress(String lastPosition, int progressRate) {
        this.lastPosition = lastPosition;
        this.progressRate = progressRate;
    }

    /**
     * 완료된 콘텐츠 여부를 반환한다.
     */
    public boolean isCompleted() {
        return this.progressRate >= COMPLETION_THRESHOLD;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

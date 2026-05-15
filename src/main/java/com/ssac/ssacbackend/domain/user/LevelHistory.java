package com.ssac.ssacbackend.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 레벨 변경 이력 엔티티.
 *
 * <p>레벨업이 발생할 때마다 이전 레벨과 새 레벨을 기록한다.
 */
@Entity
@Table(name = "level_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_level", nullable = false, length = 20)
    private UserLevel previousLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_level", nullable = false, length = 20)
    private UserLevel newLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public LevelHistory(User user, UserLevel previousLevel, UserLevel newLevel) {
        this.user = user;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

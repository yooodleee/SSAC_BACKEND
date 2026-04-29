package com.ssac.ssacbackend.domain.user;

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
 * 비회원 데이터 마이그레이션 실패 기록 엔티티.
 *
 * <p>로그인 시 guestId 기반 데이터 이전이 실패했을 때 정보를 기록한다.
 * 이후 관리자가 수동으로 처리하거나, 배치 로직이 재시도할 수 있도록 한다.
 */
@Entity
@Table(name = "migration_failures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MigrationFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String guestId;

    @Column(nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public MigrationFailure(String guestId, Long userId, String errorMessage) {
        this.guestId = guestId;
        this.userId = userId;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

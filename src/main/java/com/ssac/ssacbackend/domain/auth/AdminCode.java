package com.ssac.ssacbackend.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 사전 발급 코드 엔티티.
 *
 * <p>코드는 SHA-256 해시로 저장하여 원문 노출을 방지한다.
 * 1회 사용 후 used: true로 마킹하여 재사용을 차단한다.
 */
@Entity
@Table(name = "admin_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_hash", nullable = false, unique = true, length = 255)
    private String codeHash;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AdminCode(String codeHash, Long adminUserId, LocalDateTime expiresAt) {
        this.codeHash = codeHash;
        this.adminUserId = adminUserId;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    /**
     * 코드를 사용 처리한다. 로그인 성공 시 호출한다.
     */
    public void markAsUsed() {
        this.used = true;
    }

    /**
     * 코드가 만료되었는지 확인한다.
     *
     * <p>expiresAt은 KST(Asia/Seoul) 기준으로 저장되므로 현재 시각도 KST로 비교한다.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now(ZoneId.of("Asia/Seoul")).isAfter(expiresAt);
    }
}

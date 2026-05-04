package com.ssac.ssacbackend.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 도메인 엔티티.
 *
 * <p>변경 기준: docs/conventions.md#lombok-사용-규칙
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column // 소셜 로그인의 경우 비밀번호가 없을 수 있음
    private String password;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Column(length = 20)
    private String provider; // 예: "kakao"

    @Column(length = 100)
    private String providerId; // 소셜 서비스의 고유 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 이 시각 이전에 발급된 Access Token은 무효 처리된다.
     * 로그아웃 시 현재 시각으로 갱신하여 기존 토큰을 일괄 차단한다.
     */
    @Column
    private LocalDateTime invalidatedBefore;

    @Column
    private LocalDateTime serviceTermAgreedAt;

    @Column
    private LocalDateTime privacyTermAgreedAt;

    @Column
    private LocalDateTime ageVerificationAgreedAt;

    @Column
    private LocalDateTime marketingTermAgreedAt;

    @Builder
    public User(String email, String password, String nickname, String provider,
        String providerId, UserRole role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role != null ? role : UserRole.USER;
    }

    /**
     * 닉네임을 변경한다. 유효성 검사는 Service 레이어에서 수행한다.
     */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 사용자 권한을 변경한다. 변경된 권한은 필터의 DB 조회로 다음 요청부터 즉시 반영된다.
     */
    public void updateRole(UserRole newRole) {
        this.role = newRole;
    }

    /**
     * 현재 시각을 invalidatedBefore로 설정하여 이전에 발급된 모든 Access Token을 무효화한다.
     * 로그아웃 시 호출한다.
     */
    public void invalidateTokens() {
        this.invalidatedBefore = LocalDateTime.now();
    }

    /**
     * 약관 동의 일시를 저장한다. 회원 가입 완료 시 호출한다.
     *
     * @param serviceTermAgreedAt      서비스 이용약관 동의 일시
     * @param privacyTermAgreedAt      개인정보 처리방침 동의 일시
     * @param ageVerificationAgreedAt  만 14세 이상 확인 동의 일시
     * @param marketingTermAgreedAt    마케팅 수신 동의 일시 (선택, null 허용)
     */
    public void agreeTerms(LocalDateTime serviceTermAgreedAt,
                           LocalDateTime privacyTermAgreedAt,
                           LocalDateTime ageVerificationAgreedAt,
                           LocalDateTime marketingTermAgreedAt) {
        this.serviceTermAgreedAt = serviceTermAgreedAt;
        this.privacyTermAgreedAt = privacyTermAgreedAt;
        this.ageVerificationAgreedAt = ageVerificationAgreedAt;
        this.marketingTermAgreedAt = marketingTermAgreedAt;
    }

    @PrePersist
    private void prePersist() {
        if (this.role == null) {
            this.role = UserRole.USER;
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

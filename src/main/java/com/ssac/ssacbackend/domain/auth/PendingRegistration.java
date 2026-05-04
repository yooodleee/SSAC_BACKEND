package com.ssac.ssacbackend.domain.auth;

import com.ssac.ssacbackend.domain.social.OAuthProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 소셜 로그인 최초 시도 후 회원 가입 완료 전까지의 임시 등록 상태.
 *
 * <p>tempToken으로 식별되며 {@link com.ssac.ssacbackend.service.PendingRegistrationService}에서
 * 10분 TTL로 관리된다. 약관 동의 정보는 /api/auth/terms 요청 후 채워진다.
 */
@Getter
public class PendingRegistration {

    private final String tempToken;
    private final OAuthProvider provider;
    private final String providerUserId;
    private final String email;
    private final Instant createdAt;

    // /api/auth/terms 완료 후 채워지는 필드
    private boolean termsCompleted = false;
    private LocalDateTime serviceTermAgreedAt;
    private LocalDateTime privacyTermAgreedAt;
    private LocalDateTime ageVerificationAgreedAt;
    private LocalDateTime marketingTermAgreedAt;

    public PendingRegistration(String tempToken, OAuthProvider provider,
                               String providerUserId, String email) {
        this.tempToken = tempToken;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.createdAt = Instant.now();
    }

    /**
     * 약관 동의 정보를 설정한다.
     */
    public void completeTerms(LocalDateTime serviceTermAgreedAt,
                              LocalDateTime privacyTermAgreedAt,
                              LocalDateTime ageVerificationAgreedAt,
                              LocalDateTime marketingTermAgreedAt) {
        this.serviceTermAgreedAt = serviceTermAgreedAt;
        this.privacyTermAgreedAt = privacyTermAgreedAt;
        this.ageVerificationAgreedAt = ageVerificationAgreedAt;
        this.marketingTermAgreedAt = marketingTermAgreedAt;
        this.termsCompleted = true;
    }
}

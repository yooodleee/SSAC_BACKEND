package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.domain.social.SocialAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 소셜 계정 데이터 접근 인터페이스.
 */
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /**
     * 공급자와 공급자 사용자 ID로 소셜 계정을 조회한다.
     *
     * @param provider       OAuth 공급자 (예: NAVER)
     * @param providerUserId 공급자에서 발급한 사용자 고유 식별자
     */
    Optional<SocialAccount> findByProviderAndProviderUserId(
        OAuthProvider provider, String providerUserId);
}

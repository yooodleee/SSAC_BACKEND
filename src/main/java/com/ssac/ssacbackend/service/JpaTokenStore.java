package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.user.RefreshToken;
import com.ssac.ssacbackend.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * MySQL JPA 기반 Refresh Token 저장소.
 *
 * <p>TokenStore 인터페이스의 현재 운영 구현체.
 * Redis 전환 시 RedisTokenStore를 추가하고 이 Bean을 비활성화한다.
 */
@Component
@RequiredArgsConstructor
public class JpaTokenStore implements TokenStore {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * {@inheritDoc}
     *
     * <p>RefreshToken 엔티티를 생성하고 JPA로 영속화한다.
     */
    @Override
    @Transactional
    public void save(String hash, Long userId, Duration ttl) {
        LocalDateTime expiresAt = LocalDateTime.now().plus(ttl);
        refreshTokenRepository.save(
            RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash)
                .expiresAt(expiresAt)
                .build()
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>만료된 토큰을 발견하면 즉시 무효화하고 빈 Optional을 반환한다.
     */
    @Override
    @Transactional
    public Optional<Long> findUserIdByHash(String hash) {
        return refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
            .flatMap(token -> {
                if (token.isExpired()) {
                    token.revoke();
                    return Optional.empty();
                }
                return Optional.of(token.getUserId());
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void revoke(String hash) {
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
            .ifPresent(RefreshToken::revoke);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}

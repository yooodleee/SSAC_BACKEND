package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.domain.user.RefreshToken;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.repository.RefreshTokenRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Access Token / Refresh Token 발급·재발급·로그아웃 서비스.
 *
 * <p>Refresh Token Rotation 전략을 사용한다.
 * 재발급 시 기존 토큰을 무효화(revoked=true)하고 새 토큰을 발급한다.
 * DB에는 Refresh Token의 SHA-256 해시만 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    /**
     * 사용자에게 Access Token과 Refresh Token을 발급한다.
     */
    public TokenPair issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name()
        );
        String refreshToken = createAndStoreRefreshToken(user.getId());
        log.info("토큰 발급 완료: userId={}", user.getId());
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Refresh Token으로 새 Access Token과 Refresh Token을 재발급한다(Rotation).
     *
     * <p>기존 Refresh Token은 즉시 무효화된다.
     */
    public TokenPair reissue(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository
            .findByTokenHashAndRevokedFalse(tokenHash)
            .orElseThrow(() -> BusinessException.badRequest("유효하지 않은 Refresh Token입니다."));

        if (stored.isExpired()) {
            stored.revoke();
            throw BusinessException.badRequest("만료된 Refresh Token입니다. 다시 로그인해 주세요.");
        }

        stored.revoke();

        User user = userRepository.findById(stored.getUserId())
            .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtService.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name()
        );
        String newRefreshToken = createAndStoreRefreshToken(user.getId());
        log.info("토큰 재발급 완료: userId={}", user.getId());
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /**
     * Refresh Token을 무효화하여 로그아웃 처리한다.
     */
    public void logout(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
            .ifPresent(token -> {
                token.revoke();
                log.info("로그아웃 처리 완료: userId={}", token.getUserId());
            });
    }

    private String createAndStoreRefreshToken(Long userId) {
        String raw = jwtService.generateRefreshToken();
        String hash = hashToken(raw);
        long refreshExpirationSeconds = jwtProperties.getRefreshExpirationMs() / 1000;
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshExpirationSeconds);

        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash)
            .expiresAt(expiresAt)
            .build();
        refreshTokenRepository.save(token);
        return raw;
    }

    private String hashToken(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}

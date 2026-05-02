package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Access Token / Refresh Token 발급·재발급·로그아웃 서비스.
 *
 * <p>Refresh Token Rotation 전략을 사용한다.
 * 재발급 시 기존 토큰을 무효화하고 새 토큰을 발급한다.
 * 토큰 저장소는 {@link TokenStore} 인터페이스에만 의존하므로
 * Redis 전환 시 TokenService 수정 없이 Bean 교체만으로 동작한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {

    private final JwtService jwtService;
    private final TokenStore tokenStore;
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
     * 유효하지 않거나 만료된 토큰은 예외를 던진다.
     */
    public TokenPair reissue(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        Long userId = tokenStore.findUserIdByHash(tokenHash)
            .orElseThrow(() -> new BadRequestException(ErrorCode.TOKEN_INVALID));

        tokenStore.revoke(tokenHash);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtService.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name()
        );
        String newRefreshToken = createAndStoreRefreshToken(user.getId());
        log.info("토큰 재발급 완료: userId={}", user.getId());
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /**
     * Refresh Token을 무효화하고, 해당 사용자의 Access Token도 일괄 차단하여 로그아웃 처리한다.
     *
     * <p>user.invalidateTokens()를 호출해 invalidatedBefore를 현재 시각으로 갱신한다.
     * JwtAuthenticationFilter는 이후 요청에서 토큰의 iat와 이 값을 비교해 이전 토큰을 차단한다.
     */
    public void logout(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        tokenStore.findUserIdByHash(tokenHash).ifPresent(userId -> {
            tokenStore.revoke(tokenHash);
            userRepository.findById(userId).ifPresent(user -> {
                user.invalidateTokens();
                log.info("Access Token 무효화 완료: userId={}", userId);
            });
            log.info("로그아웃 처리 완료: userId={}", userId);
        });
    }

    /**
     * 해당 사용자의 모든 Refresh Token을 무효화하고 Access Token도 차단한다.
     *
     * <p>전체 디바이스 로그아웃 시 호출한다.
     *
     * @param email 인증된 사용자 이메일
     */
    public void logoutAll(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        tokenStore.revokeAll(user.getId());
        user.invalidateTokens();
        log.info("전체 디바이스 로그아웃 완료: userId={}", user.getId());
    }

    private String createAndStoreRefreshToken(Long userId) {
        String raw = jwtService.generateRefreshToken();
        String hash = hashToken(raw);
        Duration ttl = Duration.ofMillis(jwtProperties.getRefreshExpirationMs());
        tokenStore.save(hash, userId, ttl);
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

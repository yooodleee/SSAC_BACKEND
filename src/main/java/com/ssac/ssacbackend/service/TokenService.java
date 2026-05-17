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
 *
 * <p>재발급과 동시에 사용자 컨텍스트가 필요한 경우 {@link #reissueWithUser(String)}를 사용한다.
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
     * userId로 사용자를 조회하고 Access Token과 Refresh Token을 발급한다.
     *
     * <p>Controller 레이어가 Repository에 직접 접근하지 않도록 제공하는 편의 메서드다.
     *
     * @throws com.ssac.ssacbackend.common.exception.NotFoundException 사용자가 존재하지 않는 경우
     */
    public TokenPair issueTokensByUserId(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        return issueTokens(user);
    }

    /**
     * Refresh Token으로 새 Access Token과 Refresh Token을 재발급한다(Rotation).
     *
     * <p>기존 Refresh Token은 즉시 무효화된다.
     * 유효하지 않거나 만료된 토큰은 예외를 던진다.
     */
    public TokenPair reissue(String rawRefreshToken) {
        return reissueWithUser(rawRefreshToken).tokens();
    }

    /**
     * Refresh Token으로 새 토큰 쌍을 재발급하고, 사용자 컨텍스트를 함께 반환한다.
     *
     * <p>재접속 자동 로그인 흐름에서 FE가 필요한 사용자 정보(닉네임, 유형, 레벨, 온보딩 완료 여부)를
     * 추가 API 호출 없이 한 번에 제공한다.
     * 기존 Refresh Token은 즉시 무효화된다(Rotation).
     *
     * @param rawRefreshToken 쿠키에서 읽은 원문 Refresh Token
     * @return 새 토큰 쌍과 사용자 엔티티
     * @throws BadRequestException 토큰이 유효하지 않거나 이미 무효화된 경우 (TOKEN_INVALID)
     * @throws NotFoundException   토큰은 유효하나 사용자가 존재하지 않는 경우 (USER_NOT_FOUND)
     */
    public ReissueResult reissueWithUser(String rawRefreshToken) {
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
        return new ReissueResult(new TokenPair(newAccessToken, newRefreshToken), user);
    }

    /**
     * 토큰 재발급 결과 — 새 토큰 쌍과 사용자 엔티티를 함께 담는다.
     */
    public record ReissueResult(TokenPair tokens, User user) {}

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

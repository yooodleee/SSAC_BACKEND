package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * JWT Access Token 생성 및 검증 서비스.
 *
 * <p>JWT Payload 구조:
 * <ul>
 *   <li>sub: userId (String)</li>
 *   <li>email: 사용자 이메일</li>
 *   <li>role: 사용자 권한 (USER / ADMIN)</li>
 *   <li>iat: 발급 시각</li>
 *   <li>exp: 만료 시각</li>
 * </ul>
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(
            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
        this.expirationMs = jwtProperties.getExpirationMs();
    }

    /**
     * userId, email, role을 담은 JWT Access Token을 생성한다.
     */
    public String generateAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_EMAIL, email)
            .claim(CLAIM_ROLE, role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
    }

    /**
     * Refresh Token으로 사용할 UUID 기반 랜덤 문자열을 생성한다.
     *
     * <p>실제 저장 시에는 SHA-256 해시 값만 DB에 기록한다.
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * 토큰에서 userId를 추출한다.
     */
    public Long extractUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /**
     * 토큰에서 이메일을 추출한다.
     */
    public String extractEmail(String token) {
        return parseClaims(token).get(CLAIM_EMAIL, String.class);
    }

    /**
     * 토큰이 유효한지 검사한다. 서명 불일치·만료·형식 오류 시 false를 반환한다.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

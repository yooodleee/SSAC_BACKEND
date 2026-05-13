package com.ssac.ssacbackend.domain.auth;

import com.ssac.ssacbackend.domain.social.OAuthProvider;
import java.time.Instant;
import lombok.Getter;
import org.springframework.lang.Nullable;

/**
 * OAuth 로그인 완료 후 발급되는 일회용 인가 코드.
 *
 * <p>BE 콜백 처리 → FE 리다이렉트 → FE의 토큰 교환 요청 사이의 단계에서
 * 사용된다. TTL은 30초이며 {@link com.ssac.ssacbackend.service.AuthCodeService}에서
 * 단 한 번만 소비(consume)할 수 있다.
 *
 * <ul>
 *   <li>기존 회원: {@code userId}가 채워지고 {@code tempToken}/{@code provider}는 null</li>
 *   <li>신규 회원: {@code tempToken}/{@code provider}가 채워지고 {@code userId}는 null</li>
 * </ul>
 */
@Getter
public class AuthCode {

    private final String code;
    private final boolean newUser;
    private final Instant createdAt;

    /** 기존 회원 전용 필드 */
    @Nullable
    private final Long userId;

    /** 신규 회원 전용 필드 */
    @Nullable
    private final String tempToken;

    @Nullable
    private final OAuthProvider provider;

    private AuthCode(String code, boolean newUser, Long userId,
                     String tempToken, OAuthProvider provider) {
        this.code = code;
        this.newUser = newUser;
        this.userId = userId;
        this.tempToken = tempToken;
        this.provider = provider;
        this.createdAt = Instant.now();
    }

    public static AuthCode forExistingUser(String code, Long userId) {
        return new AuthCode(code, false, userId, null, null);
    }

    public static AuthCode forNewUser(String code, String tempToken, OAuthProvider provider) {
        return new AuthCode(code, true, null, tempToken, provider);
    }
}

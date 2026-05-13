package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.auth.AuthCode;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.dto.AuthCodeResult;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * OAuth 로그인 완료 후 FE로 전달하는 일회용 인가 코드를 관리한다.
 *
 * <p>BE 콜백 → FE 리다이렉트 구간에서 JWT/tempToken을 URL에 직접 노출하는 대신
 * 이 단기 코드(TTL {@value #TTL_SECONDS}초)를 사용한다.
 * FE는 이 코드로 {@code POST /api/v1/auth/token}을 호출해 실제 토큰을 교환한다.
 *
 * <p>코드는 단 한 번만 소비(consume)될 수 있다.
 * 소비 즉시 저장소에서 삭제되어 재사용이 불가능하다.
 */
@Slf4j
@Service
public class AuthCodeService {

    static final long TTL_SECONDS = 30L;

    private final ConcurrentHashMap<String, AuthCode> store = new ConcurrentHashMap<>();

    /**
     * 기존 회원용 인가 코드를 발급한다.
     *
     * @param userId 로그인한 회원 ID
     * @return 일회용 인가 코드 문자열
     */
    public String issueForExistingUser(Long userId) {
        purgeExpired();
        String code = UUID.randomUUID().toString();
        store.put(code, AuthCode.forExistingUser(code, userId));
        log.debug("AuthCode 발급(기존 회원): userId={}", userId);
        return code;
    }

    /**
     * 신규 회원용 인가 코드를 발급한다.
     *
     * @param tempToken    {@link PendingRegistrationService}가 발급한 임시 토큰
     * @param provider     OAuth 공급자
     * @return 일회용 인가 코드 문자열
     */
    public String issueForNewUser(String tempToken, OAuthProvider provider) {
        purgeExpired();
        String code = UUID.randomUUID().toString();
        store.put(code, AuthCode.forNewUser(code, tempToken, provider));
        log.debug("AuthCode 발급(신규 회원): provider={}", provider);
        return code;
    }

    /**
     * 신규 회원용 인가 코드를 발급한다 (Controller 레이어 전용).
     *
     * <p>Controller가 {@code domain} 패키지의 {@link OAuthProvider}를 직접 참조하지 않도록
     * 공급자 이름을 문자열로 받아 내부에서 변환한다.
     *
     * @param tempToken    {@link PendingRegistrationService}가 발급한 임시 토큰
     * @param providerName OAuth 공급자 이름 (예: "NAVER", "KAKAO")
     * @return 일회용 인가 코드 문자열
     * @throws IllegalArgumentException 알 수 없는 공급자 이름인 경우
     */
    public String issueForNewUser(String tempToken, String providerName) {
        OAuthProvider provider = OAuthProvider.valueOf(providerName);
        return issueForNewUser(tempToken, provider);
    }

    /**
     * 인가 코드를 소비한다. 유효한 경우 {@link AuthCodeResult}를 반환하고 즉시 삭제한다.
     *
     * <p>반환 타입을 {@code dto} 패키지의 DTO로 두어 Controller 레이어가
     * {@code domain} 패키지에 직접 의존하지 않도록 한다.
     *
     * @param code 클라이언트가 전달한 인가 코드
     * @return 유효한 코드이면 {@link AuthCodeResult}, 만료·미존재이면 empty
     */
    public Optional<AuthCodeResult> consume(String code) {
        AuthCode authCode = store.remove(code);
        if (authCode == null) {
            log.warn("AuthCode 조회 실패 (미존재 또는 이미 소비됨): code={}", code);
            return Optional.empty();
        }
        if (isExpired(authCode)) {
            log.warn("AuthCode 만료: code={}", code);
            return Optional.empty();
        }
        log.debug("AuthCode 소비 완료: isNewUser={}", authCode.isNewUser());
        return Optional.of(toResult(authCode));
    }

    private AuthCodeResult toResult(AuthCode authCode) {
        if (authCode.isNewUser()) {
            return AuthCodeResult.newUser(
                authCode.getTempToken(),
                authCode.getProvider().name()
            );
        }
        return AuthCodeResult.existingUser(authCode.getUserId());
    }

    private boolean isExpired(AuthCode authCode) {
        return Instant.now().isAfter(authCode.getCreatedAt().plusSeconds(TTL_SECONDS));
    }

    private void purgeExpired() {
        store.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
}

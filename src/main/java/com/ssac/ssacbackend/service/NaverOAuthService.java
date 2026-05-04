package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.config.NaverOAuthProperties;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.domain.social.SocialAccount;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.NaverLoginResult;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.response.NaverProfileResponse;
import com.ssac.ssacbackend.dto.response.NaverTokenResponse;
import com.ssac.ssacbackend.repository.SocialAccountRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 네이버 OAuth 인증 비즈니스 로직.
 *
 * <p>Authorization Code Flow를 구현한다.
 * state 파라미터로 CSRF를 방어하며, 인증 성공 시 내부 User Entity에 매핑한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverOAuthService {

    private static final String NAVER_AUTH_URL = "https://nid.naver.com/oauth2.0/authorize";
    private static final String NAVER_TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String NAVER_PROFILE_URL = "https://openapi.naver.com/v1/nid/me";
    private static final long STATE_TTL_SECONDS = 600L;
    private static final int MAX_NICKNAME_BASE_LENGTH = 16;
    private static final int NICKNAME_SUFFIX_BOUND = 10000;

    private final NaverOAuthProperties naverOAuthProperties;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final TokenService tokenService;
    private final GuestMigrationService guestMigrationService;
    private final PendingRegistrationService pendingRegistrationService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    private final ConcurrentHashMap<String, Instant> stateStore = new ConcurrentHashMap<>();

    /**
     * 네이버 OAuth 인증 URL을 생성하고 state를 저장한다.
     *
     * <p>state는 UUID로 생성되며 10분간 유효하다.
     *
     * @return 네이버 인증 페이지 URL
     */
    public String generateAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        stateStore.put(state, Instant.now());
        purgeExpiredStates();

        return UriComponentsBuilder.fromUriString(NAVER_AUTH_URL)
            .queryParam("response_type", "code")
            .queryParam("client_id", naverOAuthProperties.getClientId())
            .queryParam("redirect_uri", naverOAuthProperties.getRedirectUri())
            .queryParam("state", state)
            .build()
            .toUriString();
    }

    /**
     * 네이버 콜백을 처리하여 신규/기존 회원을 분기한다.
     *
     * <ul>
     *   <li>기존 회원: Guest 마이그레이션 수행 후 Access/Refresh Token을 발급한다.</li>
     *   <li>신규 회원: DB에 저장하지 않고 tempToken을 생성하여 회원 가입 플로우로 안내한다.</li>
     * </ul>
     *
     * @param code    네이버가 전달한 인증 코드
     * @param state   CSRF 방어용 state 파라미터
     * @param guestId 비회원 상태에서 쌓인 데이터를 이전할 guestId (없으면 null)
     * @return 신규/기존 회원 분기 결과
     */
    @Transactional
    public NaverLoginResult processCallback(String code, String state, @Nullable String guestId) {
        validateState(state);

        NaverTokenResponse tokenResponse = exchangeCodeForToken(code, state);
        NaverProfileResponse.NaverUserDetail profile = fetchNaverProfile(tokenResponse.getAccessToken());

        return socialAccountRepository
            .findByProviderAndProviderUserId(OAuthProvider.NAVER, profile.getId())
            .map(SocialAccount::getUser)
            .map(user -> {
                // 기존 회원: Guest 마이그레이션 후 토큰 발급
                if (guestId != null) {
                    log.debug("네이버 로그인 시 guestId 감지, 마이그레이션 실행: guestId={}", guestId);
                    GuestMigrationService.MigrationResult migrationResult =
                        guestMigrationService.migrateGuestData(guestId, user);
                    if (!migrationResult.success()) {
                        log.warn("Guest 마이그레이션 실패, 로그인 계속 진행: guestId={}", guestId);
                    }
                }
                TokenPair tokenPair = tokenService.issueTokens(user);
                log.info("네이버 기존 회원 로그인 완료: userId={}", user.getId());
                return NaverLoginResult.existingUser(tokenPair);
            })
            .orElseGet(() -> {
                // 신규 회원: tempToken 발급
                String email = resolveEmail(profile);
                String tempToken = pendingRegistrationService.create(
                    OAuthProvider.NAVER, profile.getId(), email);
                log.info("네이버 신규 회원 감지, tempToken 발급: naverId={}", profile.getId());
                return NaverLoginResult.newUser(tempToken);
            });
    }

    private void validateState(String state) {
        Instant created = stateStore.remove(state);
        if (created == null) {
            throw new BadRequestException(ErrorCode.OAUTH_STATE_INVALID);
        }
        if (Instant.now().isAfter(created.plusSeconds(STATE_TTL_SECONDS))) {
            throw new BadRequestException(ErrorCode.OAUTH_STATE_EXPIRED);
        }
    }

    private NaverTokenResponse exchangeCodeForToken(String code, String state) {
        String tokenUrl = UriComponentsBuilder.fromUriString(NAVER_TOKEN_URL)
            .queryParam("grant_type", "authorization_code")
            .queryParam("client_id", naverOAuthProperties.getClientId())
            .queryParam("client_secret", naverOAuthProperties.getClientSecret())
            .queryParam("code", code)
            .queryParam("state", state)
            .build()
            .toUriString();

        NaverTokenResponse response = restTemplate.getForObject(tokenUrl, NaverTokenResponse.class);
        if (response == null || response.getAccessToken() == null) {
            log.error("네이버 토큰 교환 실패: code={}", code);
            throw new BadRequestException(ErrorCode.OAUTH_AUTH_FAILED);
        }
        return response;
    }

    private NaverProfileResponse.NaverUserDetail fetchNaverProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        NaverProfileResponse response = restTemplate.exchange(
            NAVER_PROFILE_URL, HttpMethod.GET, entity, NaverProfileResponse.class
        ).getBody();

        if (response == null || !"00".equals(response.getResultcode())) {
            log.error("네이버 프로필 조회 실패");
            throw new BadRequestException(ErrorCode.OAUTH_PROFILE_FAILED);
        }
        return response.getResponse();
    }

    private String resolveEmail(NaverProfileResponse.NaverUserDetail profile) {
        if (profile.getEmail() != null && !profile.getEmail().isBlank()) {
            return profile.getEmail();
        }
        return "naver_" + profile.getId() + "@social.local";
    }

    private String resolveUniqueNickname(String rawNickname) {
        String base = sanitizeNickname(rawNickname);
        if (!userRepository.existsByNickname(base)) {
            return base;
        }
        String truncated = base.length() > MAX_NICKNAME_BASE_LENGTH
            ? base.substring(0, MAX_NICKNAME_BASE_LENGTH) : base;
        for (int i = 0; i < 10; i++) {
            String candidate = truncated
                + String.format("%04d", (int) (Math.random() * NICKNAME_SUFFIX_BOUND));
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        return "user" + (System.currentTimeMillis() % 100000);
    }

    private String sanitizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return "naver사용자";
        }
        String sanitized = nickname.replaceAll("[^a-zA-Z0-9가-힣_-]", "");
        if (sanitized.length() > 20) {
            sanitized = sanitized.substring(0, 20);
        }
        if (sanitized.length() < 2) {
            return "naver사용자";
        }
        return sanitized;
    }

    private void purgeExpiredStates() {
        Instant cutoff = Instant.now().minusSeconds(STATE_TTL_SECONDS);
        stateStore.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
}

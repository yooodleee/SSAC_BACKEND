package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.config.NaverOAuthProperties;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.domain.social.SocialAccount;
import com.ssac.ssacbackend.domain.user.User;
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
     * 네이버 콜백을 처리하고 Access Token / Refresh Token 쌍을 발급한다.
     *
     * <p>state 검증 → 인증 코드 교환 → 프로필 조회 → 사용자 조회/생성 순으로 진행된다.
     *
     * @param code  네이버가 전달한 인증 코드
     * @param state CSRF 방어용 state 파라미터
     * @return Access Token / Refresh Token 쌍
     */
    @Transactional
    public TokenPair processCallback(String code, String state) {
        validateState(state);

        NaverTokenResponse tokenResponse = exchangeCodeForToken(code, state);
        NaverProfileResponse.NaverUserDetail profile = fetchNaverProfile(tokenResponse.getAccessToken());

        User user = findOrCreateUser(profile);
        return tokenService.issueTokens(user);
    }

    private void validateState(String state) {
        Instant created = stateStore.remove(state);
        if (created == null) {
            throw BusinessException.badRequest("유효하지 않은 state 파라미터입니다.");
        }
        if (Instant.now().isAfter(created.plusSeconds(STATE_TTL_SECONDS))) {
            throw BusinessException.badRequest("만료된 state 파라미터입니다. 다시 로그인해 주세요.");
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
            throw BusinessException.badRequest("네이버 인증에 실패했습니다.");
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
            throw BusinessException.badRequest("네이버 프로필 정보를 가져오는 데 실패했습니다.");
        }
        return response.getResponse();
    }

    private User findOrCreateUser(NaverProfileResponse.NaverUserDetail profile) {
        return socialAccountRepository
            .findByProviderAndProviderUserId(OAuthProvider.NAVER, profile.getId())
            .map(SocialAccount::getUser)
            .orElseGet(() -> createUserWithSocialAccount(profile));
    }

    private User createUserWithSocialAccount(NaverProfileResponse.NaverUserDetail profile) {
        String email = resolveEmail(profile);
        String nickname = resolveUniqueNickname(profile.getNickname());
        String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = User.builder()
            .email(email)
            .password(dummyPassword)
            .nickname(nickname)
            .build();
        userRepository.save(user);

        SocialAccount socialAccount = SocialAccount.builder()
            .provider(OAuthProvider.NAVER)
            .providerUserId(profile.getId())
            .user(user)
            .build();
        socialAccountRepository.save(socialAccount);

        log.info("소셜 회원 가입 완료: provider=NAVER, naverId={}", profile.getId());
        return user;
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

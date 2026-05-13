package com.ssac.ssacbackend.config;

import com.ssac.ssacbackend.common.util.CookieUtils;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.response.KakaoUserInfo;
import com.ssac.ssacbackend.dto.response.OAuth2UserInfo;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.AuthCodeService;
import com.ssac.ssacbackend.service.GuestMigrationService;
import com.ssac.ssacbackend.service.PendingRegistrationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 카카오 OAuth2 로그인 성공 시 신규/기존 회원을 분기 처리한다.
 *
 * <ul>
 *   <li>기존 회원: 30초 TTL 일회용 authCode를 발급하고 FE 콜백으로 리다이렉트한다.</li>
 *   <li>신규 회원: tempToken을 authCode로 감싸 FE 회원 가입 플로우로 리다이렉트한다.</li>
 * </ul>
 *
 * <p>JWT와 tempToken을 리다이렉트 URL에 직접 노출하지 않는다.
 * FE는 authCode를 {@code POST /api/v1/auth/token}으로 교환해 실제 토큰을 받는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthCodeService authCodeService;
    private final GuestMigrationService guestMigrationService;
    private final UserRepository userRepository;
    private final PendingRegistrationService pendingRegistrationService;
    private final CookieProperties cookieProperties;

    @Value("${oauth2.default-redirect-uri:http://localhost:3000}")
    private String defaultRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2UserInfo oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        String providerId = oAuth2UserInfo.getProviderId();

        Optional<User> userOpt = userRepository.findByProviderAndProviderId("kakao", providerId);

        if (userOpt.isEmpty()) {
            // 신규 회원: tempToken 발급 후 회원 가입 플로우로 리다이렉트
            handleNewUser(request, response, oAuth2UserInfo);
        } else {
            // 기존 회원: 토큰 발급 후 로그인 완료
            handleExistingUser(request, response, userOpt.get());
        }
    }

    private void handleNewUser(HttpServletRequest request, HttpServletResponse response,
        OAuth2UserInfo userInfo) throws IOException {

        String email = userInfo.getEmail();
        if (email == null || email.isBlank()) {
            email = userInfo.getProviderId() + "@kakao.com";
        }

        String tempToken = pendingRegistrationService.create(
            OAuthProvider.KAKAO, userInfo.getProviderId(), email);
        String authCode = authCodeService.issueForNewUser(tempToken, OAuthProvider.KAKAO);

        log.info("카카오 신규 회원 감지, authCode 발급: providerId={}", userInfo.getProviderId());

        CookieUtils.clearRedirectToCookie(response, cookieProperties);
        String redirectUrl = defaultRedirectUri
            + "/auth/kakao/callback?authCode=" + authCode + "&isNewUser=true";
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private void handleExistingUser(HttpServletRequest request, HttpServletResponse response,
        User user) throws IOException {

        String guestId = extractCookieValue(request, "guestId");
        if (guestId != null) {
            log.debug("Kakao 로그인 시 guestId 쿠키 감지, 마이그레이션 실행: guestId={}", guestId);
            GuestMigrationService.MigrationResult migrationResult =
                guestMigrationService.migrateGuestData(guestId, user);
            if (!migrationResult.success()) {
                log.warn("Guest 마이그레이션 실패, 로그인 계속 진행: guestId={}", guestId);
            }
            CookieUtils.clearGuestIdCookie(response, cookieProperties);
        } else {
            log.debug("Kakao 로그인: guestId 쿠키 없음, 마이그레이션 생략");
        }

        String authCode = authCodeService.issueForExistingUser(user.getId());

        CookieUtils.clearRedirectToCookie(response, cookieProperties);
        String callbackUrl = defaultRedirectUri
            + "/auth/kakao/callback?authCode=" + authCode + "&isNewUser=false";
        log.info("카카오 기존 회원 로그인 성공: userId={}, authCode 발급 완료", user.getId());
        getRedirectStrategy().sendRedirect(request, response, callbackUrl);
    }

    private String extractCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

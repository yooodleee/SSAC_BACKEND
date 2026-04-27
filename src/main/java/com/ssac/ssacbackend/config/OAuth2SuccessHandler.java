package com.ssac.ssacbackend.config;

import com.ssac.ssacbackend.common.util.CookieUtils;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.response.KakaoUserInfo;
import com.ssac.ssacbackend.dto.response.OAuth2UserInfo;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.GuestMigrationService;
import com.ssac.ssacbackend.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OAuth2 로그인 성공 시 Access Token과 Refresh Token을 발급하고 쿠키에 담아 전달한다.
 *
 * <p>Access Token은 accessToken 쿠키에, Refresh Token은 refreshToken HttpOnly 쿠키에 저장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final GuestMigrationService guestMigrationService;
    private final UserRepository userRepository;
    private final CookieProperties cookieProperties;

    @Value("${oauth2.default-redirect-uri:http://localhost:3000}")
    private String defaultRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2UserInfo oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        String providerId = oAuth2UserInfo.getProviderId();

        User user = userRepository.findByProviderAndProviderId("kakao", providerId)
            .orElseGet(() -> {
                log.warn("OAuth2 로그인 성공했으나 DB에서 사용자를 찾을 수 없음: providerId={}", providerId);
                return null;
            });

        if (user == null) {
            response.sendRedirect("/?error=user_not_found");
            return;
        }

        String guestId = extractGuestIdFromCookie(request);
        if (guestId != null) {
            log.debug("Kakao 로그인 시 guestId 쿠키 감지, 마이그레이션 실행: guestId={}", guestId);
            guestMigrationService.migrateGuestData(guestId, user);
            CookieUtils.clearGuestIdCookie(response, cookieProperties);
        } else {
            log.debug("Kakao 로그인: guestId 쿠키 없음, 마이그레이션 생략");
        }

        TokenPair tokens = tokenService.issueTokens(user);

        CookieUtils.addAccessTokenCookie(response, tokens.accessToken(), cookieProperties);
        CookieUtils.addRefreshTokenCookie(response, tokens.refreshToken(), cookieProperties);

        String redirectTo = extractCookieValue(request, "redirectTo");
        CookieUtils.clearRedirectToCookie(response, cookieProperties);

        String targetUrl = StringUtils.hasText(redirectTo) ? redirectTo : defaultRedirectUri;
        log.info("OAuth2 로그인 성공: userId={}, 토큰 발급 완료, redirectTo={}", user.getId(), targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String extractGuestIdFromCookie(HttpServletRequest request) {
        return extractCookieValue(request, "guestId");
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

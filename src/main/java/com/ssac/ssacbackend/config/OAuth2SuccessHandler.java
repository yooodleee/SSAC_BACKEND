package com.ssac.ssacbackend.config;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.response.KakaoUserInfo;
import com.ssac.ssacbackend.dto.response.OAuth2UserInfo;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * OAuth2 로그인 성공 시 Access Token과 Refresh Token을 발급하고 쿠키에 담아 전달한다.
 *
 * <p>Access Token은 accessToken 쿠키에, Refresh Token은 refreshToken HttpOnly 쿠키에 저장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final int ACCESS_TOKEN_MAX_AGE = 30 * 60;       // 30분
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7일

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2UserInfo oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        String email = oAuth2UserInfo.getEmail();

        if (email == null) {
            email = oAuth2UserInfo.getProviderId() + "@kakao.com";
        }

        String resolvedEmail = email;
        User user = userRepository.findByEmail(resolvedEmail)
            .orElseGet(() -> {
                log.warn("OAuth2 로그인 성공했으나 DB에서 사용자를 찾을 수 없음: email={}", resolvedEmail);
                return null;
            });

        if (user == null) {
            response.sendRedirect("/?error=user_not_found");
            return;
        }

        TokenPair tokens = tokenService.issueTokens(user);

        Cookie accessTokenCookie = new Cookie("accessToken", tokens.accessToken());
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setMaxAge(ACCESS_TOKEN_MAX_AGE);
        // accessTokenCookie.setSecure(true); // 운영 환경(HTTPS)에서 활성화
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", tokens.refreshToken());
        refreshTokenCookie.setPath("/api/v1/auth");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        // refreshTokenCookie.setSecure(true); // 운영 환경(HTTPS)에서 활성화
        response.addCookie(refreshTokenCookie);

        log.info("OAuth2 로그인 성공: userId={}, 토큰 발급 완료", user.getId());
        getRedirectStrategy().sendRedirect(request, response, "/");
    }
}

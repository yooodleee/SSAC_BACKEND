package com.ssac.ssacbackend.config;

import com.ssac.ssacbackend.dto.response.KakaoUserInfo;
import com.ssac.ssacbackend.dto.response.OAuth2UserInfo;
import com.ssac.ssacbackend.service.JwtService;
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
 * OAuth2 로그인 성공 시 JWT 토큰을 생성하고 쿠키에 담아 전달한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2UserInfo oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        String email = oAuth2UserInfo.getEmail();
        
        if (email == null) {
            email = oAuth2UserInfo.getProviderId() + "@kakao.com";
        }

        String token = jwtService.generateToken(email);
        
        // JWT를 HttpOnly 쿠키에 저장 (보안 강화)
        Cookie cookie = new Cookie("accessToken", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(3600); // 1시간 (JwtService 설정과 맞추는 것이 좋음)
        // cookie.setSecure(true); // HTTPS 환경에서만 전송 (운영 시 활성화)
        response.addCookie(cookie);

        log.info("OAuth2 로그인 성공: {}, 쿠키에 토큰 저장 완료", email);

        getRedirectStrategy().sendRedirect(request, response, "/");
    }
}

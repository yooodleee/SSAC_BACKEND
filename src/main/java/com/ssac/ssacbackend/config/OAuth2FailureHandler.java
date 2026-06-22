package com.ssac.ssacbackend.config;

import com.ssac.ssacbackend.common.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth2 인증 실패 시 에러 메시지를 담아 리다이렉트한다.
 *
 * <p>redirectTo 쿠키가 있으면 해당 경로로, 없으면 /로 리다이렉트한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final CookieProperties cookieProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception) throws IOException {

        String provider = extractProvider(request.getRequestURI());
        log.warn("OAuth2 인증 실패: provider={}, exceptionType={}, reason={}, uri={}",
            provider,
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            request.getRequestURI());

        String redirectTo = extractCookieValue(request, "redirectTo");
        CookieUtils.clearRedirectToCookie(response, cookieProperties);

        String base = StringUtils.hasText(redirectTo) ? redirectTo : "/";
        String targetUrl = UriComponentsBuilder.fromUriString(base)
            .queryParam("error", "인증에 실패했습니다: " + exception.getMessage())
            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * 요청 URI에서 OAuth2 공급자 이름을 추출한다.
     *
     * <p>예: /login/oauth2/code/naver → "naver"
     */
    private String extractProvider(String requestUri) {
        if (requestUri == null) {
            return "unknown";
        }
        String[] parts = requestUri.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown";
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

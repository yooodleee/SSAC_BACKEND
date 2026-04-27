package com.ssac.ssacbackend.config;

import com.ssac.ssacbackend.common.util.CookieUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * OAuth2 인증 시작 시 redirectTo 쿼리 파라미터를 쿠키에 저장하는 필터.
 *
 * <p>/oauth2/authorization/** 요청에서 redirectTo 파라미터를 감지하고,
 * 유효한 경로인 경우 쿠키에 저장한다. 인증 성공/실패 핸들러가 이 쿠키를 읽어
 * 최종 리다이렉트 대상을 결정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2RedirectFilter extends OncePerRequestFilter {

    private static final String REDIRECT_TO_PARAM = "redirectTo";
    private static final String OAUTH2_AUTHORIZATION_PATH = "/oauth2/authorization/";

    private final CookieProperties cookieProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith(OAUTH2_AUTHORIZATION_PATH)) {
            String redirectTo = request.getParameter(REDIRECT_TO_PARAM);
            if (StringUtils.hasText(redirectTo) && isSafePath(redirectTo)) {
                CookieUtils.addRedirectToCookie(response, redirectTo, cookieProperties);
                log.debug("redirectTo 쿠키 저장: {}", redirectTo);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Open Redirect 방지: 절대 URL을 차단하고 상대 경로만 허용한다.
     */
    private boolean isSafePath(String redirectTo) {
        try {
            URI uri = URI.create(redirectTo);
            if (uri.isAbsolute()) {
                log.warn("절대 URL redirectTo 차단: {}", redirectTo);
                return false;
            }
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 redirectTo 차단: {}", redirectTo);
            return false;
        }
    }
}

package com.ssac.ssacbackend.config;

import com.ssac.ssacbackend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT Bearer 토큰을 검증하고 SecurityContext에 인증 정보를 설정하는 필터.
 *
 * <p>Authorization 헤더(Bearer) 또는 accessToken 쿠키에서 토큰을 추출한다.
 * 유효한 토큰이 있으면 email을 principal로 하는 Authentication을 등록한다.
 * 토큰이 없거나 유효하지 않으면 인증 없이 다음 필터로 넘기며,
 * Spring Security의 권한 검사에서 401로 거부된다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null) {
            jwtService.extractTokenInfoIfValid(token).ifPresentOrElse(
                tokenInfo -> {
                    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + tokenInfo.role());
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            tokenInfo.principal(), null, Collections.singletonList(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT 인증 성공: principal={}, role={}, uri={}",
                        tokenInfo.principal(), tokenInfo.role(), request.getRequestURI());
                },
                () -> log.warn("JWT 토큰 검증 실패 (서명 불일치 또는 만료): uri={}", request.getRequestURI())
            );
        } else {
            log.debug("JWT 토큰 없음 (비인증 요청): uri={}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

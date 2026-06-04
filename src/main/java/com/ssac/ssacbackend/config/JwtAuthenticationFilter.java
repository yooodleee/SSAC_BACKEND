package com.ssac.ssacbackend.config;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.JwtService;
import com.ssac.ssacbackend.service.JwtService.TokenInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
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
 * USER/ADMIN 토큰은 DB에서 현재 역할을 조회하여 권한 변경을 즉시 반영하고,
 * invalidatedBefore 필드로 로그아웃된 토큰을 차단한다.
 * GUEST 토큰은 DB 조회 없이 처리한다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null) {
            jwtService.extractTokenInfoIfValid(token).ifPresentOrElse(
                tokenInfo -> authenticate(tokenInfo, request),
                () -> log.warn("JWT 토큰 검증 실패 (서명 불일치 또는 만료): uri={}", request.getRequestURI())
            );
        } else {
            log.debug("JWT 토큰 없음 (비인증 요청): uri={}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(TokenInfo tokenInfo, HttpServletRequest request) {
        // GUEST는 DB 조회 없이 처리
        if (UserRole.GUEST.name().equals(tokenInfo.role())) {
            setAuthentication(tokenInfo.principal(), tokenInfo.role(), request);
            return;
        }

        // USER/ADMIN: DB에서 현재 역할 조회 및 토큰 유효성 검사
        userRepository.findByEmail(tokenInfo.principal())
            .filter(user -> isTokenStillValid(user, tokenInfo.issuedAt()))
            .ifPresentOrElse(
                user -> setAuthentication(tokenInfo.principal(), user.getRole().name(), request),
                () -> log.warn("무효화된 토큰 또는 존재하지 않는 사용자: email={}, uri={}",
                    tokenInfo.principal(), request.getRequestURI())
            );
    }

    /**
     * 토큰이 invalidatedBefore 이후에 발급된 경우에만 유효하다.
     * invalidatedBefore가 null이면 무효화된 적 없는 계정이므로 항상 유효하다.
     *
     * <p>JWT iat와 invalidatedBefore 모두 초 단위 정밀도다.
     * issuedAt >= invalidatedBefore 이면 유효한 토큰으로 판단한다(!isBefore, 경계 포함).
     * 로그아웃과 재발급이 같은 초에 발생하는 경우 새 토큰의 iat == invalidatedBefore가
     * 되므로, isAfter(>) 대신 !isBefore(>=)를 사용하여 정상 재발급 토큰이 거부되지 않도록 한다.
     */
    private boolean isTokenStillValid(User user, LocalDateTime issuedAt) {
        LocalDateTime invalidatedBefore = user.getInvalidatedBefore();
        return invalidatedBefore == null || !issuedAt.isBefore(invalidatedBefore);
    }

    private void setAuthentication(String principal, String role, HttpServletRequest request) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(authority));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("JWT 인증 성공: principal={}, role={}, uri={}",
            principal, role, request.getRequestURI());
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

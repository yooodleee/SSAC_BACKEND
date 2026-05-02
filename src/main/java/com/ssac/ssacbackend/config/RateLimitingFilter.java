package com.ssac.ssacbackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IP 기반 Rate Limiting 필터.
 *
 * <p>OAuth 인증 엔드포인트(/api/v1/auth/naver/*)에 대해 IP당 분당 최대 요청 횟수를 제한한다.
 * 제한 초과 시 HTTP 429(Too Many Requests)를 반환한다.
 *
 * <p>카운터 저장소는 {@link RateLimitStore} 인터페이스에만 의존하므로
 * Redis 전환 시 Filter 수정 없이 Bean 교체만으로 동작한다.
 * 현재 구현체({@link InMemoryRateLimitStore})의 다중 인스턴스 한계는
 * docs/decisions/006-store-abstraction.md 참고.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000L;
    private static final String RATE_LIMITED_PATH_PREFIX = "/api/v1/auth/naver";

    private final RateLimitStore rateLimitStore;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(RATE_LIMITED_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String ip = resolveClientIp(request);
        long count = rateLimitStore.increment(ip, WINDOW_MS);
        if (count > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit 초과: ip={}, uri={}", ip, request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"success\":false,\"data\":null,"
                    + "\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

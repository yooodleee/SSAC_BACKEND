package com.ssac.ssacbackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IP 기반 Rate Limiting 필터.
 *
 * <p>OAuth 인증 엔드포인트(/api/v1/auth/naver/*)에 대해 IP당 분당 최대 요청 횟수를 제한한다.
 * 제한 초과 시 HTTP 429(Too Many Requests)를 반환한다.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000L;
    private static final String RATE_LIMITED_PATH_PREFIX = "/api/v1/auth/naver";

    // IP → [윈도우 시작 시각(ms), 요청 횟수]
    private final ConcurrentHashMap<String, long[]> requestCounts = new ConcurrentHashMap<>();

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
        if (isRateLimited(ip)) {
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

    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        long[] entry = requestCounts.compute(ip, (key, val) -> {
            if (val == null || now - val[0] >= WINDOW_MS) {
                return new long[]{now, 1L};
            }
            val[1]++;
            return val;
        });
        return entry[1] > MAX_REQUESTS_PER_MINUTE;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

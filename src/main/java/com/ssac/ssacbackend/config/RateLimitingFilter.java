package com.ssac.ssacbackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IP 기반 Rate Limiting 필터.
 *
 * <p>경로별 독립 규칙({@link RateLimitRule})을 순서대로 매칭하여
 * IP당 허용 요청 횟수를 제한한다. 제한 초과 시 HTTP 429를 반환한다.
 *
 * <p>카운터 키는 "{ip}:{pathPrefix}" 형식이어서 경로별로 독립 카운터가 유지된다.
 * 카운터 저장소는 {@link RateLimitStore} 인터페이스에만 의존하므로
 * Redis 전환 시 Filter 수정 없이 Bean 교체만으로 동작한다.
 * 현재 구현체({@link InMemoryRateLimitStore})의 다중 인스턴스 한계는
 * docs/decisions/006-store-abstraction.md 참고.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    /**
     * 경로별 Rate Limiting 규칙.
     *
     * <p>목록 순서대로 매칭되며 첫 번째로 일치한 규칙이 적용된다.
     * 인증 계열은 brute-force 방지를 위해 엄격하게, 공개 조회 API는 상대적으로 관대하게 설정한다.
     */
    static final List<RateLimitRule> RULES = List.of(
        new RateLimitRule("/api/v1/auth",   10, WINDOW_MS),
        new RateLimitRule("/api/auth",       10, WINDOW_MS),
        new RateLimitRule("/login",          10, WINDOW_MS),
        new RateLimitRule("/oauth2",         10, WINDOW_MS),
        new RateLimitRule("/api/v1/feedback", 5, WINDOW_MS),
        new RateLimitRule("/api/v1/search",  60, WINDOW_MS)
    );

    private final RateLimitStore rateLimitStore;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return matchingRule(request.getRequestURI()).isEmpty();
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        RateLimitRule rule = matchingRule(uri).orElseThrow();

        String ip = resolveClientIp(request);
        String key = ip + ":" + rule.pathPrefix();
        long count = rateLimitStore.increment(key, rule.windowMs());

        if (count > rule.maxRequests()) {
            log.warn("Rate limit 초과: ip={}, uri={}", ip, uri);
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

    private Optional<RateLimitRule> matchingRule(String uri) {
        return RULES.stream().filter(r -> uri.startsWith(r.pathPrefix())).findFirst();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

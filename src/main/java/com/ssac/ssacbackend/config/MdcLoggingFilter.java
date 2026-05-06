package com.ssac.ssacbackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * HTTP 요청마다 MDC(Mapped Diagnostic Context)에 추적 정보를 주입하는 필터.
 *
 * <p>traceId, userId, requestId, method, path를 MDC에 설정하고 요청 처리 완료 후 제거한다.
 * JwtAuthenticationFilter 이후 실행되므로 인증된 사용자 ID를 정확히 추출한다.
 *
 * <p>FE가 전달한 X-Request-Id 헤더를 requestId로 사용하고, 없으면 UUID를 생성한다.
 * 생성된 traceId는 X-Trace-Id 응답 헤더로 반환되어 FE가 요청 추적에 활용할 수 있다.
 *
 * <p>보안 주의사항: MDC에는 개인 식별 정보(이메일, 전화번호)나 토큰 값을 포함하지 않는다.
 */
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_ID = "requestId";
    private static final String METHOD = "method";
    private static final String PATH = "path";
    private static final String ANONYMOUS = "anonymous";
    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String X_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        String requestId = resolveRequestId(request);
        long startTime = System.currentTimeMillis();
        try {
            MDC.put(TRACE_ID, traceId);
            MDC.put(REQUEST_ID, requestId);
            MDC.put(USER_ID, extractUserId());
            MDC.put(METHOD, request.getMethod());
            MDC.put(PATH, request.getRequestURI());
            response.setHeader(X_TRACE_ID, traceId);
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("duration", duration + "ms");
            MDC.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String headerValue = request.getHeader(X_REQUEST_ID);
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue;
        }
        return UUID.randomUUID().toString();
    }

    private String extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return ANONYMOUS;
        }
        return auth.getName();
    }
}

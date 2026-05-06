package com.ssac.ssacbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CORS 거부 요청을 감지하고 구조화된 에러 응답을 반환하는 필터.
 *
 * <p>Origin 헤더가 허용 목록에 없을 때:
 * <ul>
 *   <li>요청 Origin, 허용된 Origin 목록, 거부 사유를 WARN 로그로 기록한다</li>
 *   <li>{"status":403,"code":"CORS-001","message":"허용되지 않은 Origin입니다.",...} 응답을 반환한다</li>
 * </ul>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class CorsRejectionFilter extends OncePerRequestFilter {

    private final CorsProperties corsProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");

        if (origin != null && !isAllowedOrigin(origin)) {
            List<String> allowedOrigins = corsProperties.getAllowedOrigins();
            log.warn("CORS 거부: requestedOrigin={} | allowedOrigins={} | method={} | path={}",
                origin, allowedOrigins, request.getMethod(), request.getRequestURI());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

            Map<String, Object> body = Map.of(
                "status", 403,
                "code", "CORS-001",
                "message", "허용되지 않은 Origin입니다.",
                "requestedOrigin", origin,
                "timestamp", Instant.now().toString()
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedOrigin(String origin) {
        return corsProperties.getAllowedOrigins().contains(origin);
    }
}

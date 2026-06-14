package com.ssac.ssacbackend.config;

/**
 * Rate Limiting 경로별 규칙.
 *
 * @param pathPrefix  보호할 URI 접두사
 * @param maxRequests 허용 최대 요청 횟수 (슬라이딩 윈도우 내)
 * @param windowMs    슬라이딩 윈도우 크기(밀리초)
 */
public record RateLimitRule(String pathPrefix, int maxRequests, long windowMs) {
}

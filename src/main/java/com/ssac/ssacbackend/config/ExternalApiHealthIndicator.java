package com.ssac.ssacbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 API (카카오, 네이버) 상태를 확인하는 커스텀 헬스 인디케이터.
 *
 * <p>/actuator/health 응답의 components.externalApi 항목으로 노출된다.
 * Spring Boot 4.x 패키지: org.springframework.boot.health.contributor
 */
@Slf4j
@Component("externalApi")
public class ExternalApiHealthIndicator implements HealthIndicator {

    private static final String KAKAO_HEALTH_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String NAVER_HEALTH_URL = "https://nid.naver.com/oauth2.0/authorize";

    @Override
    public Health health() {
        String kakaoStatus = checkEndpoint(KAKAO_HEALTH_URL, "kakao");
        String naverStatus = checkEndpoint(NAVER_HEALTH_URL, "naver");

        boolean allUp = "UP".equals(kakaoStatus) && "UP".equals(naverStatus);

        Health.Builder builder = allUp ? Health.up() : Health.down();
        return builder
            .withDetail("kakao", kakaoStatus)
            .withDetail("naver", naverStatus)
            .build();
    }

    private String checkEndpoint(String url, String name) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getForObject(url, String.class);
            return "UP";
        } catch (Exception e) {
            // 4xx 응답(redirect 포함)도 연결 성공으로 간주 — 실제 인증 없이 도달 가능한지만 확인
            String msg = e.getMessage();
            if (msg != null && (msg.contains("302") || msg.contains("400") || msg.contains("401")
                || msg.contains("403") || msg.contains("404"))) {
                return "UP";
            }
            log.warn("외부 API 상태 확인 실패: name={}, error={}", name, msg);
            return "DOWN";
        }
    }
}

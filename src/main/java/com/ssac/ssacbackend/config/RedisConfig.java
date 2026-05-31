package com.ssac.ssacbackend.config;

import org.springframework.context.annotation.Configuration;

/**
 * Redis 설정.
 *
 * <p>{@code StringRedisTemplate}은 Spring Boot 자동 구성으로 등록된다.
 * 콘텐츠 목록 캐시는 {@code NotionSyncService}에서,
 * 홈 화면 캐시는 {@code HomeService}에서 {@code StringRedisTemplate}과 {@code ObjectMapper}로 직접 관리한다.
 */
@Configuration
public class RedisConfig {
}

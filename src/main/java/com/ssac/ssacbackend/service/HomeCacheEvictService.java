package com.ssac.ssacbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 홈 화면 Redis 캐시 무효화 서비스.
 *
 * <p>콘텐츠 완료, 퀴즈 완료, 레벨 변경, 관심 도메인 변경 시 캐시를 무효화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeCacheEvictService {

    static final String HOME_CACHE_PREFIX = "home:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 특정 사용자의 홈 캐시를 무효화한다.
     */
    public void evict(Long userId) {
        try {
            redisTemplate.delete(HOME_CACHE_PREFIX + userId);
            log.debug("홈 캐시 무효화: userId={}", userId);
        } catch (RedisConnectionFailureException e) {
            log.warn("홈 캐시 무효화 실패 (Redis 연결 오류) - 무시하고 진행: userId={}", userId);
        }
    }
}

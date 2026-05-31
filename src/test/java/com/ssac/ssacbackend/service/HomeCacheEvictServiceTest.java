package com.ssac.ssacbackend.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class HomeCacheEvictServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private HomeCacheEvictService homeCacheEvictService;

    @Test
    @DisplayName("evict - 정상: 홈 캐시 키를 삭제한다")
    void evict_정상() {
        homeCacheEvictService.evict(1L);

        verify(stringRedisTemplate).delete(HomeCacheEvictService.HOME_CACHE_PREFIX + 1L);
    }

    @Test
    @DisplayName("evict - Redis 연결 실패 시 예외 없이 처리를 계속한다")
    void evict_Redis연결실패_예외무시() {
        doThrow(new RedisConnectionFailureException("연결 실패"))
            .when(stringRedisTemplate).delete(HomeCacheEvictService.HOME_CACHE_PREFIX + 99L);

        homeCacheEvictService.evict(99L);
        // 예외가 전파되지 않으면 성공
    }
}

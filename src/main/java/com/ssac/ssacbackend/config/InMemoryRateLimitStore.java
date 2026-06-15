package com.ssac.ssacbackend.config;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ConcurrentHashMap 기반 인메모리 Rate Limiting 저장소.
 *
 * <p>RateLimitStore 인터페이스의 현재 운영 구현체.
 *
 * <p><b>알려진 한계 (KNOWN-LIMIT-001)</b><br>
 * 단일 JVM 인스턴스 내에서만 정확히 동작한다.
 * 로드밸런서로 여러 인스턴스에 분산 배포 시 각 인스턴스가 독립 카운터를 유지하여
 * 실제 허용 요청 수가 {@code MAX_REQUESTS × 인스턴스 수}가 될 수 있다.
 * 다중 인스턴스 환경에서는 RedisRateLimitStore로 교체할 것.
 * 상세 내용: docs/decisions/006-store-abstraction.md
 *
 * <p><b>OOM 방어</b><br>
 * 만료 엔트리는 60초마다 {@link #evictExpired()}가 일괄 제거한다.
 * 엔트리 수가 {@value #MAX_ENTRIES}를 초과하면 만료 엔트리를 즉시 퇴출하여 상한을 보장한다.
 */
@Component
public class InMemoryRateLimitStore implements RateLimitStore {

    static final int MAX_ENTRIES = 100_000;

    // key → [윈도우 시작 시각(ms), 요청 횟수, 윈도우 크기(ms)]
    private final ConcurrentHashMap<String, long[]> requestCounts = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>슬라이딩 윈도우가 만료된 경우 카운터를 초기화하고 1을 반환한다.
     * 엔트리 수가 {@value #MAX_ENTRIES}를 초과하면 만료 엔트리를 즉시 퇴출한다.
     */
    @Override
    public long increment(String key, long windowMs) {
        long now = System.currentTimeMillis();

        if (requestCounts.size() >= MAX_ENTRIES) {
            evictExpired();
        }

        long[] entry = requestCounts.compute(key, (k, val) -> {
            if (val == null || now - val[0] >= windowMs) {
                return new long[]{now, 1L, windowMs};
            }
            val[1]++;
            return val;
        });
        return entry[1];
    }

    /**
     * 만료된 엔트리를 Map에서 제거한다.
     *
     * <p>60초마다 스케줄러가 자동 호출한다. 공격 등으로 {@value #MAX_ENTRIES}를 초과한 경우
     * {@link #increment}에서도 즉시 호출된다.
     */
    @Scheduled(fixedDelay = 60_000L)
    public void evictExpired() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(e -> {
            long[] val = e.getValue();
            return now - val[0] >= val[2];
        });
    }
}

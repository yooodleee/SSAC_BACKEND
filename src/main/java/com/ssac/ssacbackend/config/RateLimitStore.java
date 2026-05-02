package com.ssac.ssacbackend.config;

/**
 * Rate Limiting 카운터 저장소 추상화 인터페이스.
 *
 * <p>현재 구현체: {@link InMemoryRateLimitStore} (ConcurrentHashMap 기반 단일 인스턴스 전용)
 * <br>전환 예정: RedisRateLimitStore — 다중 인스턴스 환경에서 정확한 Rate Limiting 보장
 *
 * <p><b>다중 서버 주의사항</b><br>
 * 인메모리 구현체는 각 서버 인스턴스가 독립적인 카운터를 유지하므로
 * 로드밸런서 환경에서 사용자가 여러 인스턴스에 분산될 경우 실제 제한보다
 * 더 많은 요청이 허용될 수 있다. 상세 내용은 docs/decisions/006-store-abstraction.md 참고.
 */
public interface RateLimitStore {

    /**
     * 지정된 키의 요청 카운터를 증가시키고 현재 카운트를 반환한다.
     *
     * <p>슬라이딩 윈도우가 만료된 경우 카운터를 초기화하고 1을 반환한다.
     *
     * @param key      클라이언트 식별자 (예: IP 주소)
     * @param windowMs 슬라이딩 윈도우 크기(밀리초)
     * @return 현재 윈도우 내 누적 요청 횟수
     */
    long increment(String key, long windowMs);
}

package com.ssac.ssacbackend.service;

import java.time.Duration;
import java.util.Optional;

/**
 * Refresh Token 저장소 추상화 인터페이스.
 *
 * <p>현재 구현체: {@link JpaTokenStore} (MySQL JPA 기반)
 * <br>전환 예정: RedisTokenStore — Bean만 교체하면 TokenService 수정 없이 동작한다.
 *
 * <p>저장소 교체 절차:
 * <ol>
 *   <li>RedisTokenStore 구현체 추가 (implements TokenStore)</li>
 *   <li>JpaTokenStore Bean에 @Primary 제거 또는 @ConditionalOnProperty 조건 추가</li>
 *   <li>TokenService 코드 수정 없이 동작 확인</li>
 * </ol>
 */
public interface TokenStore {

    /**
     * Refresh Token 해시를 저장한다.
     *
     * @param hash   SHA-256 해시 값
     * @param userId 토큰 소유자 ID
     * @param ttl    토큰 유효 기간
     */
    void save(String hash, Long userId, Duration ttl);

    /**
     * 해시 값으로 유효한 토큰의 소유자 ID를 조회한다.
     *
     * <p>토큰이 존재하지 않거나, 이미 무효화됐거나, 만료된 경우 빈 Optional을 반환한다.
     *
     * @param hash SHA-256 해시 값
     * @return 유효한 토큰의 소유자 ID, 없으면 empty
     */
    Optional<Long> findUserIdByHash(String hash);

    /**
     * 특정 해시의 Refresh Token을 무효화한다(단일 디바이스 로그아웃).
     *
     * @param hash SHA-256 해시 값
     */
    void revoke(String hash);

    /**
     * 사용자의 모든 Refresh Token을 무효화한다(전체 디바이스 로그아웃).
     *
     * @param userId 사용자 ID
     */
    void revokeAll(Long userId);
}

package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.user.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Refresh Token JPA 리포지토리.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 해시 값으로 유효한(revoked=false) Refresh Token을 조회한다.
     */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /**
     * 해시 값으로 Refresh Token을 조회한다 (revoked 여부 무관).
     *
     * <p>Token Rotation 경쟁 조건 처리에 사용한다.
     * 첫 번째 reissue가 토큰을 교체한 직후, 동시 요청이 이미 교체된 토큰으로 재시도할 때
     * 만료되지 않은 revoked 토큰의 userId를 조회하여 새 토큰을 발급한다.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 특정 사용자의 모든 Refresh Token을 무효화한다. (강제 로그아웃 시 사용)
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    void deleteByTokenHash(String tokenHash);

    void deleteByUserId(Long userId);
}

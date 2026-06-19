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
     * <p>revokeIfActive 성공 후 userId 조회에 사용한다.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 유효한(revoked=false, 만료 전) 토큰을 원자적으로 무효화한다(Token Rotation 전용).
     *
     * <p>DB 레벨 단일 UPDATE로 경쟁 조건을 방어한다.
     * 영향받은 행 수(0 또는 1)를 반환하여 호출자가 성공 여부를 판단하게 한다.
     *
     * @param hash SHA-256 해시 값
     * @return 업데이트된 행 수 (1이면 성공, 0이면 토큰 없음·이미 무효화·만료)
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true "
        + "WHERE r.tokenHash = :hash AND r.revoked = false AND r.expiresAt > CURRENT_TIMESTAMP")
    int revokeIfActive(@Param("hash") String hash);

    /**
     * 특정 사용자의 모든 Refresh Token을 무효화한다. (강제 로그아웃 시 사용)
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    void deleteByTokenHash(String tokenHash);

    void deleteByUserId(Long userId);
}

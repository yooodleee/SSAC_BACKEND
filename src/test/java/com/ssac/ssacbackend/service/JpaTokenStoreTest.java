package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.user.RefreshToken;
import com.ssac.ssacbackend.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaTokenStoreTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private JpaTokenStore jpaTokenStore;

    @Test
    @DisplayName("save - RefreshToken 엔티티를 생성하고 저장한다")
    void save_정상() {
        jpaTokenStore.save("hash-abc", 1L, Duration.ofDays(7));

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("findUserIdByHash - 유효한 토큰이면 userId를 반환한다")
    void findUserIdByHash_유효한토큰() {
        RefreshToken token = RefreshToken.builder()
            .userId(42L)
            .tokenHash("valid-hash")
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
        given(refreshTokenRepository.findByTokenHashAndRevokedFalse("valid-hash"))
            .willReturn(Optional.of(token));

        Optional<Long> result = jpaTokenStore.findUserIdByHash("valid-hash");

        assertThat(result).contains(42L);
    }

    @Test
    @DisplayName("findUserIdByHash - 만료된 토큰이면 revoke 후 empty를 반환한다")
    void findUserIdByHash_만료된토큰() {
        RefreshToken token = RefreshToken.builder()
            .userId(42L)
            .tokenHash("expired-hash")
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
        given(refreshTokenRepository.findByTokenHashAndRevokedFalse("expired-hash"))
            .willReturn(Optional.of(token));

        Optional<Long> result = jpaTokenStore.findUserIdByHash("expired-hash");

        assertThat(result).isEmpty();
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("findUserIdByHash - 존재하지 않는 토큰이면 empty를 반환한다")
    void findUserIdByHash_없는토큰() {
        given(refreshTokenRepository.findByTokenHashAndRevokedFalse("unknown"))
            .willReturn(Optional.empty());

        Optional<Long> result = jpaTokenStore.findUserIdByHash("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("revoke - 토큰이 존재하면 revoked=true로 무효화한다")
    void revoke_정상() {
        RefreshToken token = RefreshToken.builder()
            .userId(1L)
            .tokenHash("to-revoke")
            .expiresAt(LocalDateTime.now().plusDays(1))
            .build();
        given(refreshTokenRepository.findByTokenHashAndRevokedFalse("to-revoke"))
            .willReturn(Optional.of(token));

        jpaTokenStore.revoke("to-revoke");

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("revoke - 토큰이 없으면 아무 동작도 하지 않는다")
    void revoke_없는토큰() {
        given(refreshTokenRepository.findByTokenHashAndRevokedFalse("missing"))
            .willReturn(Optional.empty());

        jpaTokenStore.revoke("missing");
        // 예외 없이 종료되면 성공
    }

    @Test
    @DisplayName("revokeAll - 사용자의 모든 토큰을 무효화한다")
    void revokeAll_정상() {
        jpaTokenStore.revokeAll(1L);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }
}

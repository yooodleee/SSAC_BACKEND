package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private TokenService tokenService;

    // ── issueTokens ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("토큰 발급(issueTokens)")
    class IssueTokens {

        @Test
        @DisplayName("정상 사용자로 발급 시 Access Token과 Refresh Token을 반환한다")
        void 정상_토큰_발급() {
            User user = mockUser(1L, "test@test.com", UserRole.USER);
            given(jwtService.generateAccessToken(1L, "test@test.com", "USER"))
                .willReturn("access-token");
            given(jwtService.generateRefreshToken()).willReturn("raw-refresh");
            given(jwtProperties.getRefreshExpirationMs()).willReturn(604800000L);

            TokenPair pair = tokenService.issueTokens(user);

            assertThat(pair.accessToken()).isEqualTo("access-token");
            assertThat(pair.refreshToken()).isEqualTo("raw-refresh");
        }

        @Test
        @DisplayName("발급 시 TokenStore.save가 올바른 userId와 ttl로 호출된다")
        void 발급_시_TokenStore_save_호출() {
            User user = mockUser(1L, "test@test.com", UserRole.USER);
            given(jwtService.generateAccessToken(anyLong(), anyString(), anyString()))
                .willReturn("access-token");
            given(jwtService.generateRefreshToken()).willReturn("raw-refresh");
            given(jwtProperties.getRefreshExpirationMs()).willReturn(604800000L);

            tokenService.issueTokens(user);

            then(tokenStore).should().save(anyString(), eq(1L),
                eq(Duration.ofMillis(604800000L)));
        }
    }

    // ── reissue ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("토큰 재발급(reissue)")
    class Reissue {

        @Test
        @DisplayName("유효한 Refresh Token으로 새 TokenPair를 반환한다")
        void 유효한_토큰_재발급_성공() {
            User user = mockUser(2L, "user@test.com", UserRole.USER);
            given(tokenStore.findUserIdByHash(anyString())).willReturn(Optional.of(2L));
            given(userRepository.findById(2L)).willReturn(Optional.of(user));
            given(jwtService.generateAccessToken(2L, "user@test.com", "USER"))
                .willReturn("new-access");
            given(jwtService.generateRefreshToken()).willReturn("new-refresh");
            given(jwtProperties.getRefreshExpirationMs()).willReturn(604800000L);

            TokenPair pair = tokenService.reissue("raw-old-refresh");

            assertThat(pair.accessToken()).isEqualTo("new-access");
            assertThat(pair.refreshToken()).isEqualTo("new-refresh");
        }

        @Test
        @DisplayName("재발급 시 기존 Refresh Token이 무효화된다(Rotation)")
        void 재발급_시_기존_토큰_무효화() {
            User user = mockUser(2L, "user@test.com", UserRole.USER);
            given(tokenStore.findUserIdByHash(anyString())).willReturn(Optional.of(2L));
            given(userRepository.findById(2L)).willReturn(Optional.of(user));
            given(jwtService.generateAccessToken(anyLong(), anyString(), anyString()))
                .willReturn("new-access");
            given(jwtService.generateRefreshToken()).willReturn("new-refresh");
            given(jwtProperties.getRefreshExpirationMs()).willReturn(604800000L);

            tokenService.reissue("raw-old-refresh");

            then(tokenStore).should().revoke(anyString());
        }

        @Test
        @DisplayName("유효하지 않거나 만료된 Refresh Token이면 400 예외가 발생한다")
        void 유효하지_않은_토큰_재발급_실패() {
            given(tokenStore.findUserIdByHash(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> tokenService.reissue("invalid-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                    assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("토큰은 유효하지만 사용자가 삭제된 경우 404 예외가 발생한다")
        void 토큰_유효하나_사용자_없으면_예외() {
            given(tokenStore.findUserIdByHash(anyString())).willReturn(Optional.of(99L));
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> tokenService.reissue("valid-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                    assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // ── logout ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("로그아웃(logout)")
    class Logout {

        @Test
        @DisplayName("유효한 Refresh Token으로 로그아웃 시 토큰이 무효화된다")
        void 유효한_토큰_로그아웃() {
            User user = mock(User.class);
            given(tokenStore.findUserIdByHash(anyString())).willReturn(Optional.of(3L));
            given(userRepository.findById(3L)).willReturn(Optional.of(user));

            tokenService.logout("raw-refresh");

            then(tokenStore).should().revoke(anyString());
            then(user).should().invalidateTokens();
        }

        @Test
        @DisplayName("이미 만료·무효화된 Refresh Token이면 아무 동작도 하지 않는다")
        void 무효화된_토큰_로그아웃_노옵() {
            given(tokenStore.findUserIdByHash(anyString())).willReturn(Optional.empty());

            tokenService.logout("expired-token");

            then(tokenStore).should(never()).revoke(anyString());
        }
    }

    // ── logoutAll ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("전체 디바이스 로그아웃(logoutAll)")
    class LogoutAll {

        @Test
        @DisplayName("사용자의 모든 Refresh Token이 무효화된다")
        void 전체_토큰_무효화() {
            User user = mock(User.class);
            given(user.getId()).willReturn(4L);
            given(userRepository.findByEmail("all@test.com")).willReturn(Optional.of(user));

            tokenService.logoutAll("all@test.com");

            then(tokenStore).should().revokeAll(4L);
            then(user).should().invalidateTokens();
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 404 예외가 발생한다")
        void 존재하지_않는_이메일_예외() {
            given(userRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> tokenService.logoutAll("ghost@test.com"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                    assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private User mockUser(Long id, String email, UserRole role) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        given(user.getEmail()).willReturn(email);
        given(user.getRole()).willReturn(role);
        return user;
    }
}

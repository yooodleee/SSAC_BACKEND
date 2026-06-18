package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.UnauthorizedException;
import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.RegisterV2Result;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.request.EmailLoginRequest;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * EmailAuthService 단위 테스트.
 *
 * <p>이메일+비밀번호 로그인 성공/실패 분기를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EmailAuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private EmailAuthService emailAuthService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
            .email("user@test.com")
            .password("encoded-password")
            .nickname("테스터")
            .build();
    }

    @Nested
    @DisplayName("loginWithEmail 성공")
    class Success {

        @Test
        @DisplayName("이메일과 비밀번호가 일치하면 토큰을 발급한다")
        void loginWithEmail_성공() {
            given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("raw-password", "encoded-password")).willReturn(true);
            given(tokenService.issueTokens(mockUser)).willReturn(new TokenPair("access", "refresh"));
            given(jwtProperties.getExpirationMs()).willReturn(3600000L);

            RegisterV2Result result = emailAuthService.loginWithEmail(
                new EmailLoginRequest("user@test.com", "raw-password"));

            assertThat(result.refreshToken()).isEqualTo("refresh");
            assertThat(result.response().accessToken()).isEqualTo("access");
        }
    }

    @Nested
    @DisplayName("loginWithEmail 실패 — AUTH-011")
    class Failure {

        @Test
        @DisplayName("이메일이 존재하지 않으면 AUTH-011을 던진다")
        void loginWithEmail_이메일_미존재() {
            given(userRepository.findByEmail(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> emailAuthService.loginWithEmail(
                new EmailLoginRequest("none@test.com", "pw")))
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_OR_PASSWORD_INVALID);
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 AUTH-011을 던진다")
        void loginWithEmail_비밀번호_불일치() {
            given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);

            assertThatThrownBy(() -> emailAuthService.loginWithEmail(
                new EmailLoginRequest("user@test.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_OR_PASSWORD_INVALID);
        }

        @Test
        @DisplayName("소셜 전용 계정(password=null)으로 이메일 로그인 시 AUTH-011을 던진다")
        void loginWithEmail_소셜계정_비밀번호_null() {
            User socialUser = User.builder()
                .email("social@test.com")
                .password(null)
                .nickname("소셜유저")
                .build();
            given(userRepository.findByEmail("social@test.com")).willReturn(Optional.of(socialUser));

            assertThatThrownBy(() -> emailAuthService.loginWithEmail(
                new EmailLoginRequest("social@test.com", "any-pw")))
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_OR_PASSWORD_INVALID);
        }
    }
}

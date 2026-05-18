package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.RegisterV2Result;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.request.EmailLoginRequest;
import com.ssac.ssacbackend.dto.request.EmailRegisterRequest;
import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.repository.SocialAccountRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class EmailAuthServiceTest {

    @Mock private PendingRegistrationService pendingRegistrationService;
    @Mock private UserRepository userRepository;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private TokenService tokenService;
    @Mock private GuestMigrationService guestMigrationService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProperties jwtProperties;

    @InjectMocks
    private RegistrationService registrationService;

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private EmailRegisterRequest validRegisterRequest() {
        return new EmailRegisterRequest(
            "홍길동",
            "1995-06-15",
            "010-1234-5678",
            "MALE",
            "test@gmail.com",
            "pass1234",
            new EmailRegisterRequest.Agreements(true, true, true, false),
            null
        );
    }

    private User mockSavedUser() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(user.getName()).willReturn("홍길동");
        return user;
    }

    // ── registerWithEmail 테스트 ──────────────────────────────────────────────

    @Test
    @DisplayName("이메일 회원가입 정상 처리")
    void registerWithEmail_정상() {
        User savedUser = mockSavedUser();
        given(userRepository.existsByEmail("test@gmail.com")).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(userRepository.save(any())).willReturn(savedUser);
        given(passwordEncoder.encode(anyString())).willReturn("hashed");
        given(tokenService.issueTokens(savedUser)).willReturn(new TokenPair("access", "refresh"));
        given(jwtProperties.getExpirationMs()).willReturn(3600000L);

        RegisterV2Result result = registrationService.registerWithEmail(validRegisterRequest());

        assertThat(result.response().accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
    }

    @Test
    @DisplayName("비밀번호 형식 오류 — PASSWORD-001 발생")
    void registerWithEmail_비밀번호_형식_오류() {
        EmailRegisterRequest request = new EmailRegisterRequest(
            "홍길동", "1995-06-15", "010-1234-5678", null,
            "test@gmail.com", "short1",
            new EmailRegisterRequest.Agreements(true, true, true, false), null
        );

        assertThatThrownBy(() -> registrationService.registerWithEmail(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode().getCode())
                .isEqualTo("PASSWORD-001"));
    }

    @Test
    @DisplayName("비밀번호 숫자 없음 — PASSWORD-001 발생")
    void registerWithEmail_비밀번호_숫자_없음() {
        EmailRegisterRequest request = new EmailRegisterRequest(
            "홍길동", "1995-06-15", "010-1234-5678", null,
            "test@gmail.com", "onlyletters",
            new EmailRegisterRequest.Agreements(true, true, true, false), null
        );

        assertThatThrownBy(() -> registrationService.registerWithEmail(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode().getCode())
                .isEqualTo("PASSWORD-001"));
    }

    @Test
    @DisplayName("비밀번호 영문 없음 — PASSWORD-001 발생")
    void registerWithEmail_비밀번호_영문_없음() {
        EmailRegisterRequest request = new EmailRegisterRequest(
            "홍길동", "1995-06-15", "010-1234-5678", null,
            "test@gmail.com", "12345678",
            new EmailRegisterRequest.Agreements(true, true, true, false), null
        );

        assertThatThrownBy(() -> registrationService.registerWithEmail(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode().getCode())
                .isEqualTo("PASSWORD-001"));
    }

    @Test
    @DisplayName("만 14세 미만 — BIRTH-002 발생")
    void registerWithEmail_만14세_미만() {
        EmailRegisterRequest request = new EmailRegisterRequest(
            "홍길동", "2020-01-01", "010-1234-5678", null,
            "test@gmail.com", "pass1234",
            new EmailRegisterRequest.Agreements(true, true, true, false), null
        );

        assertThatThrownBy(() -> registrationService.registerWithEmail(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode().getCode())
                .isEqualTo("BIRTH-002"));
    }

    @Test
    @DisplayName("필수 약관 미동의 — TERMS-001 발생")
    void registerWithEmail_필수약관_미동의() {
        EmailRegisterRequest request = new EmailRegisterRequest(
            "홍길동", "1995-06-15", "010-1234-5678", null,
            "test@gmail.com", "pass1234",
            new EmailRegisterRequest.Agreements(false, true, true, false), null
        );

        assertThatThrownBy(() -> registrationService.registerWithEmail(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode().getCode())
                .isEqualTo("TERMS-001"));
    }

    @Test
    @DisplayName("이메일 중복 — EMAIL-002 발생")
    void registerWithEmail_이메일_중복() {
        given(userRepository.existsByEmail("test@gmail.com")).willReturn(true);

        assertThatThrownBy(() -> registrationService.registerWithEmail(validRegisterRequest()))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.CONFLICT.value()));
    }

    @Test
    @DisplayName("성별 null 허용 — 정상 가입")
    void registerWithEmail_성별_null_허용() {
        EmailRegisterRequest request = new EmailRegisterRequest(
            "홍길동", "1995-06-15", "010-1234-5678", null,
            "test@gmail.com", "pass1234",
            new EmailRegisterRequest.Agreements(true, true, true, false), null
        );
        User savedUser = mockSavedUser();
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(userRepository.save(any())).willReturn(savedUser);
        given(passwordEncoder.encode(anyString())).willReturn("hashed");
        given(tokenService.issueTokens(savedUser)).willReturn(new TokenPair("access", "refresh"));
        given(jwtProperties.getExpirationMs()).willReturn(3600000L);

        RegisterV2Result result = registrationService.registerWithEmail(request);

        assertThat(result.response().accessToken()).isEqualTo("access");
    }

    // ── loginWithEmail 테스트 ─────────────────────────────────────────────────

    @Test
    @DisplayName("이메일 로그인 정상 처리")
    void loginWithEmail_정상() {
        User user = mockSavedUser();
        given(user.getPassword()).willReturn("$2a$hashed");
        given(userRepository.findByEmail("test@gmail.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pass1234", "$2a$hashed")).willReturn(true);
        given(tokenService.issueTokens(user)).willReturn(new TokenPair("access", "refresh"));
        given(jwtProperties.getExpirationMs()).willReturn(3600000L);

        RegisterV2Result result = registrationService.loginWithEmail(
            new EmailLoginRequest("test@gmail.com", "pass1234")
        );

        assertThat(result.response().accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 — AUTH-011 발생")
    void loginWithEmail_이메일_없음() {
        given(userRepository.findByEmail("none@gmail.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.loginWithEmail(
            new EmailLoginRequest("none@gmail.com", "pass1234")))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode().getCode())
                .isEqualTo("AUTH-011"));
    }

    @Test
    @DisplayName("비밀번호 불일치 — AUTH-011 발생")
    void loginWithEmail_비밀번호_불일치() {
        User user = mock(User.class);
        given(user.getPassword()).willReturn("$2a$hashed");
        given(userRepository.findByEmail("test@gmail.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongpass", "$2a$hashed")).willReturn(false);

        assertThatThrownBy(() -> registrationService.loginWithEmail(
            new EmailLoginRequest("test@gmail.com", "wrongpass")))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode().getCode())
                .isEqualTo("AUTH-011"));
    }

    @Test
    @DisplayName("소셜 전용 계정 비밀번호 로그인 시도 — AUTH-011 발생")
    void loginWithEmail_소셜_전용_계정() {
        User user = mock(User.class);
        given(user.getPassword()).willReturn(null);
        given(userRepository.findByEmail("social@gmail.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> registrationService.loginWithEmail(
            new EmailLoginRequest("social@gmail.com", "pass1234")))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode().getCode())
                .isEqualTo("AUTH-011"));
    }
}

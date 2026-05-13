package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.domain.auth.PendingRegistration;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.request.RegisterRequest;
import com.ssac.ssacbackend.dto.response.RegisterResponse;
import com.ssac.ssacbackend.repository.SocialAccountRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private PendingRegistrationService pendingRegistrationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private GuestMigrationService guestMigrationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    private PendingRegistration completedPending;

    @BeforeEach
    void setUp() {
        completedPending = new PendingRegistration("valid-token", OAuthProvider.KAKAO,
            "provider-123", "user@test.com");
        completedPending.completeTerms(
            LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null);
    }

    // ── 고3 학생 유형으로 회원가입 성공 ──────────────────────────────────────────

    @Test
    @DisplayName("고3 학생 유형(HIGH_SCHOOL)으로 회원가입 시 userType이 응답에 포함된다")
    void 고3학생_유형_회원가입_성공() {
        given(pendingRegistrationService.findValid("valid-token"))
            .willReturn(Optional.of(completedPending));
        given(userRepository.existsByNickname("닉네임A")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded-pw");

        User savedUser = mock(User.class);
        given(savedUser.getId()).willReturn(1L);
        given(savedUser.getNickname()).willReturn("닉네임A");
        given(savedUser.getUserType()).willReturn(UserType.HIGH_SCHOOL);
        given(savedUser.getLevel()).willReturn(null);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(tokenService.issueTokens(savedUser))
            .willReturn(new TokenPair("access-token", "refresh-token"));

        RegisterRequest request = new RegisterRequest("valid-token", "닉네임A", UserType.HIGH_SCHOOL, null);
        RegisterResponse response = registrationService.register(request);

        assertThat(response.user().userType()).isEqualTo(UserType.HIGH_SCHOOL);
        assertThat(response.user().level()).isNull();
        assertThat(response.user().onboardingCompleted()).isFalse();
    }

    // ── 사회초년생 유형으로 회원가입 성공 ────────────────────────────────────────

    @Test
    @DisplayName("사회초년생 유형(EARLY_CAREER)으로 회원가입 시 userType이 응답에 포함된다")
    void 사회초년생_유형_회원가입_성공() {
        given(pendingRegistrationService.findValid("valid-token"))
            .willReturn(Optional.of(completedPending));
        given(userRepository.existsByNickname("닉네임B")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded-pw");

        User savedUser = mock(User.class);
        given(savedUser.getId()).willReturn(2L);
        given(savedUser.getNickname()).willReturn("닉네임B");
        given(savedUser.getUserType()).willReturn(UserType.EARLY_CAREER);
        given(savedUser.getLevel()).willReturn(null);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(tokenService.issueTokens(savedUser))
            .willReturn(new TokenPair("access-token", "refresh-token"));

        RegisterRequest request = new RegisterRequest("valid-token", "닉네임B", UserType.EARLY_CAREER, null);
        RegisterResponse response = registrationService.register(request);

        assertThat(response.user().userType()).isEqualTo(UserType.EARLY_CAREER);
        assertThat(response.user().onboardingCompleted()).isFalse();
    }

    // ── userType 누락 시 400 ──────────────────────────────────────────────────

    @Test
    @DisplayName("userType이 null이면 BadRequestException(USER-TYPE-001)이 발생한다")
    void userType_누락_시_400_응답() {
        given(pendingRegistrationService.findValid("valid-token"))
            .willReturn(Optional.of(completedPending));

        RegisterRequest request = new RegisterRequest("valid-token", "닉네임C", null, null);

        assertThatThrownBy(() -> registrationService.register(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(be.getCode()).isEqualTo("USER-TYPE-001");
            });
    }

    // ── 회원가입 세션 만료 시 401 ─────────────────────────────────────────────

    @Test
    @DisplayName("tempToken이 없거나 만료된 경우 UnauthorizedException이 발생한다")
    void 세션_만료_시_401_응답() {
        given(pendingRegistrationService.findValid("expired-token"))
            .willReturn(Optional.empty());

        RegisterRequest request = new RegisterRequest("expired-token", "닉네임D", UserType.HIGH_SCHOOL, null);

        assertThatThrownBy(() -> registrationService.register(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── 약관 미동의 시 400 ────────────────────────────────────────────────────

    @Test
    @DisplayName("약관 동의 미완료 상태에서 회원가입 시 BadRequestException이 발생한다")
    void 약관_미동의_시_400_응답() {
        PendingRegistration incompleteTerms = new PendingRegistration(
            "valid-token", OAuthProvider.KAKAO, "provider-456", "user2@test.com");
        // termsCompleted = false (기본값)
        given(pendingRegistrationService.findValid("valid-token"))
            .willReturn(Optional.of(incompleteTerms));

        RegisterRequest request = new RegisterRequest("valid-token", "닉네임E", UserType.HIGH_SCHOOL, null);

        assertThatThrownBy(() -> registrationService.register(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ── 회원가입 완료 응답에 userType 포함 확인 ───────────────────────────────

    @Test
    @DisplayName("회원가입 완료 응답의 merged.quizCount는 guest 미병합 시 0이다")
    void 회원가입_완료_응답_mergedQuizCount_기본값_0() {
        given(pendingRegistrationService.findValid("valid-token"))
            .willReturn(Optional.of(completedPending));
        given(userRepository.existsByNickname("닉네임F")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded-pw");

        User savedUser = mock(User.class);
        given(savedUser.getId()).willReturn(3L);
        given(savedUser.getNickname()).willReturn("닉네임F");
        given(savedUser.getUserType()).willReturn(UserType.HIGH_SCHOOL);
        given(savedUser.getLevel()).willReturn(null);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(tokenService.issueTokens(savedUser))
            .willReturn(new TokenPair("access-token", "refresh-token"));

        RegisterRequest request = new RegisterRequest("valid-token", "닉네임F", UserType.HIGH_SCHOOL, null);
        RegisterResponse response = registrationService.register(request);

        assertThat(response.merged().quizCount()).isZero();
        assertThat(response.user().userType()).isEqualTo(UserType.HIGH_SCHOOL);
    }

    // ── onboardingCompleted 상태 확인 ─────────────────────────────────────────

    @Test
    @DisplayName("회원가입 직후 level이 null이므로 onboardingCompleted는 false이다")
    void 회원가입_직후_onboardingCompleted_false() {
        given(pendingRegistrationService.findValid("valid-token"))
            .willReturn(Optional.of(completedPending));
        given(userRepository.existsByNickname("닉네임G")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded-pw");

        User savedUser = mock(User.class);
        given(savedUser.getId()).willReturn(4L);
        given(savedUser.getNickname()).willReturn("닉네임G");
        given(savedUser.getUserType()).willReturn(UserType.EARLY_CAREER);
        given(savedUser.getLevel()).willReturn(null);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(tokenService.issueTokens(savedUser))
            .willReturn(new TokenPair("access-token", "refresh-token"));

        RegisterRequest request = new RegisterRequest("valid-token", "닉네임G", UserType.EARLY_CAREER, null);
        RegisterResponse response = registrationService.register(request);

        assertThat(response.user().onboardingCompleted()).isFalse();
    }
}

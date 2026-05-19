package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.domain.user.Gender;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.RegisterV2Result;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.request.EmailLoginRequest;
import com.ssac.ssacbackend.repository.SocialAccountRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * RegistrationService 닉네임 표시 관련 단위 테스트.
 *
 * <p>닉네임 미설정 시 displayNickname이 name을 반환하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceNicknameTest {

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
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    @DisplayName("이메일 로그인 응답에 닉네임 포함 (닉네임 미설정 시 name 반환)")
    void loginWithEmail_nicknameNotSet_returnsName() {
        User user = User.builder()
            .email("test@example.com")
            .password("encoded")
            .nickname("test1234abcd")
            .build();
        user.completeSignup("홍길동", LocalDate.of(2000, 1, 1), "01012345678", Gender.MALE);

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", "encoded")).willReturn(true);
        given(tokenService.issueTokens(user)).willReturn(new TokenPair("access", "refresh"));
        given(jwtProperties.getExpirationMs()).willReturn(1800000L);

        RegisterV2Result result = registrationService.loginWithEmail(
            new EmailLoginRequest("test@example.com", "password"));

        // nicknameExplicitlySet = false → displayNickname = name
        assertThat(result.response().user().nickname()).isEqualTo("홍길동");
        assertThat(result.response().user().name()).isEqualTo("홍길동");
        assertThat(result.response().user().email()).isEqualTo("test@example.com");
    }
}

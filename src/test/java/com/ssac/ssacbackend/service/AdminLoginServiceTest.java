package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.UnauthorizedException;
import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.domain.auth.AdminCode;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.repository.AdminCodeRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.AdminLoginService.LoginResult;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminLoginServiceTest {

    @Mock
    private AdminCodeRepository adminCodeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenService tokenService;
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AdminLoginService adminLoginService;

    @Test
    @DisplayName("유효한 관리자 코드로 로그인 성공")
    void 유효한_관리자_코드_로그인_성공() {
        String rawCode = "valid-admin-code-123";
        String hash = AdminLoginService.sha256(rawCode);

        AdminCode adminCode = buildAdminCode(hash, 1L, false, null);
        User admin = buildAdminUser(1L);

        given(adminCodeRepository.findByCodeHash(hash)).willReturn(Optional.of(adminCode));
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(tokenService.issueTokens(admin)).willReturn(new TokenPair("access-token", "refresh-token"));
        given(jwtProperties.getExpirationMs()).willReturn(1800000L);

        LoginResult result = adminLoginService.login(rawCode);

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.response().user().role()).isEqualTo("ADMIN");
        assertThat(result.response().user().redirectTo()).isEqualTo("/admin");
    }

    @Test
    @DisplayName("유효하지 않은 코드 시 401 / ADMIN-001")
    void 유효하지_않은_코드_시_401_ADMIN_001() {
        given(adminCodeRepository.findByCodeHash(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminLoginService.login("wrong-code"))
            .isInstanceOf(UnauthorizedException.class)
            .satisfies(ex -> assertThat(((UnauthorizedException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_CODE_INVALID));
    }

    @Test
    @DisplayName("만료된 코드 시 401 / ADMIN-002")
    void 만료된_코드_시_401_ADMIN_002() {
        String rawCode = "expired-code";
        String hash = AdminLoginService.sha256(rawCode);
        AdminCode expired = buildAdminCode(hash, 1L, false, LocalDateTime.now().minusHours(1));

        given(adminCodeRepository.findByCodeHash(hash)).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> adminLoginService.login(rawCode))
            .isInstanceOf(UnauthorizedException.class)
            .satisfies(ex -> assertThat(((UnauthorizedException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_CODE_EXPIRED));
    }

    @Test
    @DisplayName("1회 사용 후 동일 코드 재사용 시 401")
    void 사용된_코드_재사용_시_401() {
        String rawCode = "used-code";
        String hash = AdminLoginService.sha256(rawCode);
        AdminCode usedCode = buildAdminCode(hash, 1L, true, null);

        given(adminCodeRepository.findByCodeHash(hash)).willReturn(Optional.of(usedCode));

        assertThatThrownBy(() -> adminLoginService.login(rawCode))
            .isInstanceOf(UnauthorizedException.class)
            .satisfies(ex -> assertThat(((UnauthorizedException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_CODE_INVALID));
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private AdminCode buildAdminCode(String hash, Long adminUserId, boolean used, LocalDateTime expiresAt) {
        AdminCode code = AdminCode.builder()
            .codeHash(hash)
            .adminUserId(adminUserId)
            .expiresAt(expiresAt)
            .build();
        if (used) {
            code.markAsUsed();
        }
        return code;
    }

    private User buildAdminUser(Long id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        given(user.getDisplayNickname()).willReturn("관리자");
        given(user.getRole()).willReturn(UserRole.ADMIN);
        return user;
    }
}

package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.auth.AdminCode;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.response.AdminCodeCreateResponse;
import com.ssac.ssacbackend.repository.AdminCodeRepository;
import com.ssac.ssacbackend.repository.FeedbackRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminCodeCreateServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private AdminCodeRepository adminCodeRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("ADMIN 사용자에게 코드 발급 성공 - rawCode 반환")
    void ADMIN_사용자_코드_발급_성공() {
        User admin = buildUser(1L, UserRole.ADMIN);
        AdminCode savedCode = buildAdminCode(10L, 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(adminCodeRepository.save(any(AdminCode.class))).willReturn(savedCode);

        AdminCodeCreateResponse response = adminService.createAdminCode(1L, null);

        assertThat(response.codeId()).isEqualTo("10");
        assertThat(response.rawCode()).isNotBlank();
        assertThat(response.adminUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("만료 일시 포함 코드 발급 성공 - KST OffsetDateTime 반환")
    void 만료일시_포함_코드_발급_성공() {
        OffsetDateTime expiresAt = OffsetDateTime.of(2026, 7, 17, 23, 0, 0, 0, ZoneOffset.of("+09:00"));
        User admin = buildUser(1L, UserRole.ADMIN);
        AdminCode savedCode = buildAdminCode(11L, 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(adminCodeRepository.save(any(AdminCode.class))).willReturn(savedCode);

        AdminCodeCreateResponse response = adminService.createAdminCode(1L, expiresAt);

        assertThat(response.expiresAt()).isNotNull();
        assertThat(response.expiresAt().getOffset()).isEqualTo(ZoneOffset.of("+09:00"));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID 시 404")
    void 존재하지_않는_사용자_ID_시_404() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createAdminCode(999L, null))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("ADMIN 역할이 아닌 사용자 ID 시 400")
    void ADMIN_역할_아닌_사용자_시_400() {
        User normalUser = buildUser(2L, UserRole.USER);
        given(userRepository.findById(2L)).willReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> adminService.createAdminCode(2L, null))
            .isInstanceOf(BadRequestException.class);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private User buildUser(Long id, UserRole role) {
        User user = mock(User.class);
        given(user.getRole()).willReturn(role);
        return user;
    }

    private AdminCode buildAdminCode(Long id, Long adminUserId) {
        AdminCode code = AdminCode.builder()
            .codeHash("some-hash")
            .adminUserId(adminUserId)
            .build();
        ReflectionTestUtils.setField(code, "id", id);
        ReflectionTestUtils.setField(code, "createdAt", LocalDateTime.now());
        return code;
    }
}

package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.domain.auth.AdminCode;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.response.AdminCodeCreateResponse;
import com.ssac.ssacbackend.dto.response.UserSummaryResponse;
import com.ssac.ssacbackend.repository.AdminCodeRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminCodeRepository adminCodeRepository;

    @InjectMocks
    private AdminService adminService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // ── 관리자 코드 발급 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("UTC OffsetDateTime으로 expiresAt을 전달하면 KST LocalDateTime으로 변환하여 저장한다")
    void createAdminCode_UTC_입력_시_KST로_변환_저장() {
        User admin = mockAdminUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        // UTC 14:00 = KST 23:00
        OffsetDateTime utcInput = OffsetDateTime.of(2026, 6, 17, 14, 0, 0, 0, ZoneOffset.UTC);
        LocalDateTime expectedKst = LocalDateTime.of(2026, 6, 17, 23, 0, 0);

        AdminCode savedCode = mockSavedAdminCode();
        ArgumentCaptor<AdminCode> captor = ArgumentCaptor.forClass(AdminCode.class);
        given(adminCodeRepository.save(captor.capture())).willReturn(savedCode);

        AdminCodeCreateResponse response = adminService.createAdminCode(1L, utcInput);

        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expectedKst);
        assertThat(response.expiresAt().toLocalDateTime()).isEqualTo(expectedKst);
        assertThat(response.expiresAt().getOffset()).isEqualTo(ZoneOffset.of("+09:00"));
    }

    @Test
    @DisplayName("KST OffsetDateTime으로 expiresAt을 전달하면 동일한 KST LocalDateTime으로 저장한다")
    void createAdminCode_KST_입력_시_동일값_저장() {
        User admin = mockAdminUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        OffsetDateTime kstInput = OffsetDateTime.of(2026, 6, 17, 23, 0, 0, 0, ZoneOffset.of("+09:00"));
        LocalDateTime expectedKst = LocalDateTime.of(2026, 6, 17, 23, 0, 0);

        AdminCode savedCode = mockSavedAdminCode();
        ArgumentCaptor<AdminCode> captor = ArgumentCaptor.forClass(AdminCode.class);
        given(adminCodeRepository.save(captor.capture())).willReturn(savedCode);

        adminService.createAdminCode(1L, kstInput);

        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expectedKst);
    }

    @Test
    @DisplayName("expiresAt이 null이면 무기한 코드로 저장한다")
    void createAdminCode_expiresAt_null_무기한_저장() {
        User admin = mockAdminUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        AdminCode savedCode = mockSavedAdminCode();
        ArgumentCaptor<AdminCode> captor = ArgumentCaptor.forClass(AdminCode.class);
        given(adminCodeRepository.save(captor.capture())).willReturn(savedCode);

        AdminCodeCreateResponse response = adminService.createAdminCode(1L, null);

        assertThat(captor.getValue().getExpiresAt()).isNull();
        assertThat(response.expiresAt()).isNull();
    }

    @Test
    @DisplayName("대상 사용자가 ADMIN 역할이 아니면 예외가 발생한다")
    void createAdminCode_ADMIN_아닌_사용자_예외() {
        User user = mock(User.class);
        given(user.getRole()).willReturn(UserRole.USER);
        given(userRepository.findById(2L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.createAdminCode(2L, null))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private User mockAdminUser() {
        User admin = mock(User.class);
        given(admin.getRole()).willReturn(UserRole.ADMIN);
        return admin;
    }

    private AdminCode mockSavedAdminCode() {
        AdminCode savedCode = mock(AdminCode.class);
        given(savedCode.getId()).willReturn(1L);
        given(savedCode.getCreatedAt()).willReturn(LocalDateTime.now(KST));
        return savedCode;
    }

    // ── 사용자 목록 조회 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("사용자 목록 조회 시 페이지네이션된 UserSummaryResponse를 반환한다")
    void 사용자_목록_조회_성공() {
        User user = buildUser(1L, "user@test.com", "닉네임", UserRole.USER);
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        given(userRepository.findAll(pageable)).willReturn(userPage);

        Page<UserSummaryResponse> result = adminService.listUsers(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).email()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("사용자가 없으면 빈 페이지를 반환한다")
    void 사용자_없을_때_빈_페이지_반환() {
        Pageable pageable = PageRequest.of(0, 20);
        given(userRepository.findAll(pageable)).willReturn(Page.empty(pageable));

        Page<UserSummaryResponse> result = adminService.listUsers(pageable);

        assertThat(result.isEmpty()).isTrue();
    }

    // ── 권한 변경 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("유효한 사용자 ID와 USER 역할로 권한 변경 시 성공한다")
    void 사용자_권한을_USER로_변경_성공() {
        User user = buildUser(1L, "user@test.com", "닉네임", UserRole.ADMIN);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        adminService.updateUserRole(1L, UserRole.USER);

        then(user).should().updateRole(UserRole.USER);
    }

    @Test
    @DisplayName("유효한 사용자 ID와 ADMIN 역할로 권한 변경 시 성공한다")
    void 사용자_권한을_ADMIN으로_변경_성공() {
        User user = buildUser(2L, "admin@test.com", "관리자", UserRole.USER);
        given(userRepository.findById(2L)).willReturn(Optional.of(user));

        adminService.updateUserRole(2L, UserRole.ADMIN);

        then(user).should().updateRole(UserRole.ADMIN);
    }

    @Test
    @DisplayName("GUEST 역할로 변경 시도 시 BusinessException(BAD_REQUEST)이 발생한다")
    void GUEST_역할_변경_시_예외_발생() {
        assertThatThrownBy(() -> adminService.updateUserRole(1L, UserRole.GUEST))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 권한 변경 시 BusinessException(NOT_FOUND)이 발생한다")
    void 존재하지_않는_사용자_권한_변경_시_예외_발생() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserRole(999L, UserRole.USER))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private User buildUser(Long id, String email, String nickname, UserRole role) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        given(user.getEmail()).willReturn(email);
        given(user.getNickname()).willReturn(nickname);
        given(user.getRole()).willReturn(role);
        return user;
    }
}

package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.domain.feedback.FeedbackStatus;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.request.AdminCodeCreateRequest;
import com.ssac.ssacbackend.dto.request.FeedbackStatusUpdateRequest;
import com.ssac.ssacbackend.dto.request.UpdateRoleRequest;
import com.ssac.ssacbackend.dto.response.AdminCodeCreateResponse;
import com.ssac.ssacbackend.dto.response.AdminHomeResponse;
import com.ssac.ssacbackend.service.AdminFeedbackService;
import com.ssac.ssacbackend.service.AdminService;
import org.springframework.data.domain.Page;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private AdminFeedbackService adminFeedbackService;

    @InjectMocks
    private AdminController controller;

    @Test
    @DisplayName("createAdminCode - 관리자 코드 발급 성공 시 201을 반환한다")
    void createAdminCode_정상() {
        AdminCodeCreateRequest request = new AdminCodeCreateRequest(1L, null);
        AdminCodeCreateResponse mockResponse = new AdminCodeCreateResponse(
            "code-id-001", "raw-code-abc", 1L, null, OffsetDateTime.now());
        given(adminService.createAdminCode(1L, null)).willReturn(mockResponse);

        ResponseEntity<?> result = controller.createAdminCode(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(adminService).createAdminCode(1L, null);
    }

    @Test
    @DisplayName("getHome - 관리자 홈 조회 성공 시 200을 반환한다")
    void getHome_정상() {
        Authentication auth = mockAuth("admin@test.com");
        AdminHomeResponse mockResponse = new AdminHomeResponse(
            new AdminHomeResponse.AdminInfo("관리자", "ADMIN"),
            new AdminHomeResponse.Stats(100L, 20L, 5L)
        );
        given(adminService.getAdminHome("admin@test.com")).willReturn(mockResponse);

        ResponseEntity<?> result = controller.getHome(auth);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminService).getAdminHome("admin@test.com");
    }

    @Test
    @DisplayName("getFeedbacks - 피드백 목록 조회 성공 시 200을 반환한다")
    void getFeedbacks_정상() {
        given(adminFeedbackService.getFeedbacks(null, 1, 20)).willReturn(null);

        ResponseEntity<?> result = controller.getFeedbacks(null, 1, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminFeedbackService).getFeedbacks(null, 1, 20);
    }

    @Test
    @DisplayName("updateFeedbackStatus - 피드백 상태 변경 성공 시 204를 반환한다")
    void updateFeedbackStatus_정상() {
        FeedbackStatusUpdateRequest request = new FeedbackStatusUpdateRequest(FeedbackStatus.DONE);

        ResponseEntity<Void> result = controller.updateFeedbackStatus(1L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(adminFeedbackService).updateStatus(1L, FeedbackStatus.DONE);
    }

    @Test
    @DisplayName("listUsers - 사용자 목록 조회 성공 시 200을 반환한다")
    void listUsers_정상() {
        Authentication auth = mockAuth("admin@test.com");
        given(adminService.listUsers(any())).willReturn(Page.empty());

        ResponseEntity<?> result = controller.listUsers(auth, 1, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminService).listUsers(any());
    }

    @Test
    @DisplayName("updateUserRole - 사용자 권한 변경 성공 시 200을 반환한다")
    void updateUserRole_정상() {
        Authentication auth = mockAuth("admin@test.com");
        UpdateRoleRequest request = new UpdateRoleRequest(UserRole.ADMIN);
        given(adminService.updateUserRole(10L, UserRole.ADMIN)).willReturn(null);

        ResponseEntity<?> result = controller.updateUserRole(auth, 10L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminService).updateUserRole(10L, UserRole.ADMIN);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }
}

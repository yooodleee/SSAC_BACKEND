package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.NotificationListResponse;
import com.ssac.ssacbackend.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@DisplayName("NotificationController")
class NotificationControllerTest {

    private NotificationService notificationService;
    private NotificationController controller;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        controller = new NotificationController(notificationService);
    }

    // ── getNotifications ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getNotifications - 알림 목록 조회")
    class GetNotifications {

        @Test
        @DisplayName("알림 목록 조회 성공 시 200과 NotificationListResponse를 반환한다")
        void getNotifications_성공() {
            Authentication auth = mockAuth("user@test.com");
            NotificationListResponse mockResponse = new NotificationListResponse(2L, List.of());
            given(notificationService.getNotifications("user@test.com")).willReturn(mockResponse);

            ResponseEntity<ApiResponse<NotificationListResponse>> result =
                controller.getNotifications(auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(mockResponse);
        }

        @Test
        @DisplayName("알림이 없는 경우 unreadCount 0으로 반환한다")
        void getNotifications_빈목록() {
            Authentication auth = mockAuth("user@test.com");
            NotificationListResponse emptyResponse = new NotificationListResponse(0L, List.of());
            given(notificationService.getNotifications("user@test.com")).willReturn(emptyResponse);

            ResponseEntity<ApiResponse<NotificationListResponse>> result =
                controller.getNotifications(auth);

            assertThat(result.getBody().getData().unreadCount()).isZero();
        }
    }

    // ── markAsRead ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAsRead - 알림 읽음 처리")
    class MarkAsRead {

        @Test
        @DisplayName("알림 읽음 처리 성공 시 204를 반환한다")
        void markAsRead_성공() {
            Authentication auth = mockAuth("user@test.com");

            ResponseEntity<Void> result = controller.markAsRead(5L, auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(notificationService).markAsRead(5L, "user@test.com");
        }
    }

    // ── markAllAsRead ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAllAsRead - 모든 알림 읽음 처리")
    class MarkAllAsRead {

        @Test
        @DisplayName("전체 읽음 처리 성공 시 204를 반환한다")
        void markAllAsRead_성공() {
            Authentication auth = mockAuth("user@test.com");

            ResponseEntity<Void> result = controller.markAllAsRead(auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(notificationService).markAllAsRead("user@test.com");
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }
}

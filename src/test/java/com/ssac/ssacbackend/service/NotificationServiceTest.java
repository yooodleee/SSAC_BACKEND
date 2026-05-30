package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.notification.Notification;
import com.ssac.ssacbackend.dto.response.NotificationListResponse;
import com.ssac.ssacbackend.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("getNotifications - 알림 목록과 읽지 않은 수를 반환한다")
    void getNotifications_정상() {
        Notification n = buildNotification(1L, "새 알림", false);
        given(notificationRepository.findByUserEmailOrderByCreatedAtDesc("user@test.com"))
            .willReturn(List.of(n));
        given(notificationRepository.countByUserEmailAndIsReadFalse("user@test.com"))
            .willReturn(1L);

        NotificationListResponse result = notificationService.getNotifications("user@test.com");

        assertThat(result.unreadCount()).isEqualTo(1L);
        assertThat(result.notifications()).hasSize(1);
        assertThat(result.notifications().get(0).message()).isEqualTo("새 알림");
    }

    @Test
    @DisplayName("getNotifications - 알림이 없으면 빈 목록과 unreadCount 0을 반환한다")
    void getNotifications_빈목록() {
        given(notificationRepository.findByUserEmailOrderByCreatedAtDesc("user@test.com"))
            .willReturn(List.of());
        given(notificationRepository.countByUserEmailAndIsReadFalse("user@test.com"))
            .willReturn(0L);

        NotificationListResponse result = notificationService.getNotifications("user@test.com");

        assertThat(result.unreadCount()).isZero();
        assertThat(result.notifications()).isEmpty();
    }

    @Test
    @DisplayName("markAsRead - 알림을 읽음 처리한다")
    void markAsRead_정상() {
        Notification n = buildNotification(1L, "알림", false);
        given(notificationRepository.findByIdAndUserEmail(1L, "user@test.com"))
            .willReturn(Optional.of(n));

        notificationService.markAsRead(1L, "user@test.com");

        assertThat(n.isRead()).isTrue();
    }

    @Test
    @DisplayName("markAsRead - 존재하지 않는 알림이면 NotFoundException을 던진다")
    void markAsRead_존재하지않음() {
        given(notificationRepository.findByIdAndUserEmail(99L, "user@test.com"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L, "user@test.com"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("markAllAsRead - 사용자의 모든 알림을 읽음 처리한다")
    void markAllAsRead_정상() {
        notificationService.markAllAsRead("user@test.com");

        verify(notificationRepository).markAllAsReadByUserEmail("user@test.com");
    }

    private Notification buildNotification(Long id, String message, boolean isRead) {
        Notification n = Notification.builder().message(message).build();
        ReflectionTestUtils.setField(n, "id", id);
        ReflectionTestUtils.setField(n, "isRead", isRead);
        return n;
    }
}

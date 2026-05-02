package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.notification.Notification;
import com.ssac.ssacbackend.dto.response.NotificationItemResponse;
import com.ssac.ssacbackend.dto.response.NotificationListResponse;
import com.ssac.ssacbackend.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 서비스.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 사용자의 알림 목록과 읽지 않은 알림 수를 반환한다.
     */
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(String email) {
        List<NotificationItemResponse> items = notificationRepository
            .findByUserEmailOrderByCreatedAtDesc(email)
            .stream()
            .map(NotificationItemResponse::from)
            .toList();
        long unreadCount = notificationRepository.countByUserEmailAndIsReadFalse(email);
        return new NotificationListResponse(unreadCount, items);
    }

    /**
     * 특정 알림을 읽음 처리한다.
     */
    @Transactional
    public void markAsRead(Long notificationId, String email) {
        Notification notification = notificationRepository
            .findByIdAndUserEmail(notificationId, email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markAsRead();
    }

    /**
     * 사용자의 모든 알림을 읽음 처리한다.
     */
    @Transactional
    public void markAllAsRead(String email) {
        notificationRepository.markAllAsReadByUserEmail(email);
    }
}

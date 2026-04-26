package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.notification.Notification;
import java.time.LocalDateTime;

/**
 * 단일 알림 응답 DTO.
 */
public record NotificationItemResponse(
    String id,
    String message,
    boolean isRead,
    LocalDateTime createdAt
) {
    public static NotificationItemResponse from(Notification notification) {
        return new NotificationItemResponse(
            notification.getId().toString(),
            notification.getMessage(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}

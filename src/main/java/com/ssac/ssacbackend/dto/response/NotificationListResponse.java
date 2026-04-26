package com.ssac.ssacbackend.dto.response;

import java.util.List;

/**
 * 알림 목록 응답 DTO.
 */
public record NotificationListResponse(
    long unreadCount,
    List<NotificationItemResponse> notifications
) {}

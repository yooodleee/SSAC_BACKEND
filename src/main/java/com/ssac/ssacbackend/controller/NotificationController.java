package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.NotificationListResponse;
import com.ssac.ssacbackend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 API.
 *
 * <p>인증된 사용자만 접근 가능하다.
 */
@Slf4j
@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
        summary = "알림 목록 조회",
        description = "읽지 않은 알림 수와 전체 알림 목록을 반환한다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
        Authentication authentication) {
        log.debug("알림 목록 조회: email={}", authentication.getName());
        NotificationListResponse result =
            notificationService.getNotifications(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
        summary = "알림 읽음 처리",
        description = "특정 알림의 isRead를 true로 갱신한다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
        @PathVariable Long id,
        Authentication authentication) {
        log.debug("알림 읽음 처리: id={}, email={}", id, authentication.getName());
        notificationService.markAsRead(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
        summary = "모든 알림 읽음 처리",
        description = "사용자의 모든 알림을 읽음 처리한다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        log.debug("모든 알림 읽음 처리: email={}", authentication.getName());
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

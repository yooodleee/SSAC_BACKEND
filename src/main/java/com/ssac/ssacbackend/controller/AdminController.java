package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.domain.feedback.FeedbackStatus;
import com.ssac.ssacbackend.dto.request.AdminCodeCreateRequest;
import com.ssac.ssacbackend.dto.request.FeedbackStatusUpdateRequest;
import com.ssac.ssacbackend.dto.request.UpdateRoleRequest;
import com.ssac.ssacbackend.dto.response.AdminCodeCreateResponse;
import com.ssac.ssacbackend.dto.response.AdminHomeResponse;
import com.ssac.ssacbackend.dto.response.FeedbackListResponse;
import com.ssac.ssacbackend.dto.response.UserSummaryResponse;
import com.ssac.ssacbackend.service.AdminFeedbackService;
import com.ssac.ssacbackend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 엔드포인트.
 *
 * <p>모든 엔드포인트는 ADMIN 역할이 있어야 접근 가능하다 (SecurityConfig에서 제한).
 */
@Slf4j
@Tag(name = "Admin", description = "관리자 전용 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminFeedbackService adminFeedbackService;

    // ── 관리자 코드 발급 ──────────────────────────────────────────────────────

    @Operation(
        summary = "관리자 코드 발급",
        description = "[권한 조건] ADMIN 역할 전용. "
            + "발급된 rawCode는 이 응답에서 단 한 번만 노출된다. "
            + "adminUserId는 ADMIN 역할을 가진 사용자여야 한다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "코드 발급 성공 (rawCode 1회 노출)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "대상 사용자가 ADMIN 역할이 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "존재하지 않는 사용자")
    })
    @PostMapping("/codes")
    public ResponseEntity<ApiResponse<AdminCodeCreateResponse>> createAdminCode(
        @RequestBody AdminCodeCreateRequest request
    ) {
        AdminCodeCreateResponse response = adminService.createAdminCode(
            request.adminUserId(), request.expiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── 관리자 홈 ──────────────────────────────────────────────────────────────

    @Operation(
        summary = "관리자 홈 화면",
        description = "[권한 조건] ADMIN 역할 전용.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<AdminHomeResponse>> getHome(Authentication authentication) {
        AdminHomeResponse response = adminService.getAdminHome(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── 피드백 목록 ────────────────────────────────────────────────────────────

    @Operation(
        summary = "피드백 목록 조회",
        description = "[권한 조건] ADMIN 역할 전용. createdAt 내림차순 정렬. 상태 필터링 지원.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackListResponse>> getFeedbacks(
        @Parameter(description = "상태 필터 (PENDING | IN_PROGRESS | DONE), 미입력 시 전체")
        @RequestParam(required = false) FeedbackStatus status,
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(adminFeedbackService.getFeedbacks(status, page, size))
        );
    }

    // ── 피드백 상태 변경 ──────────────────────────────────────────────────────

    @Operation(
        summary = "피드백 상태 변경",
        description = "[권한 조건] ADMIN 역할 전용. 변경 성공 시 204 No Content.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204", description = "상태 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "FEEDBACK-003: 존재하지 않는 피드백")
    })
    @PatchMapping("/feedbacks/{feedbackId}/status")
    public ResponseEntity<Void> updateFeedbackStatus(
        @PathVariable Long feedbackId,
        @RequestBody FeedbackStatusUpdateRequest request
    ) {
        adminFeedbackService.updateStatus(feedbackId, request.status());
        return ResponseEntity.noContent().build();
    }

    // ── 사용자 관리 ──────────────────────────────────────────────────────────

    @Operation(
        summary = "전체 사용자 목록 조회",
        description = "[권한 조건] ADMIN 역할 전용. 가입일시 내림차순 정렬. GUEST 제외.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> listUsers(
        Authentication authentication,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size) {
        log.debug("관리자 사용자 목록 조회: adminEmail={}, page={}, size={}",
            authentication.getName(), page, size);
        Page<UserSummaryResponse> result = adminService.listUsers(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── [임시] Sentry 연동 검증 — 검증 완료 후 반드시 제거 ────────────────────

    @Operation(
        summary = "[임시] Sentry 연동 검증",
        description = "[권한 조건] ADMIN 역할 전용. 의도적 5xx 예외를 발생시켜 Sentry 이벤트 수집을 확인한다."
            + " 검증 완료 후 반드시 제거한다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/sentry-test")
    public void sentryTest() {
        throw new RuntimeException("[Sentry 연동 검증] 의도적 5xx 예외");
    }

    // ── 사용자 관리 ──────────────────────────────────────────────────────────

    @Operation(
        summary = "사용자 권한 변경",
        description = "[권한 조건] ADMIN 역할 전용. GUEST 역할 부여 불가.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateUserRole(
        Authentication authentication,
        @Parameter(description = "대상 사용자 ID", example = "1")
        @PathVariable Long userId,
        @RequestBody @Valid UpdateRoleRequest request) {
        log.debug("사용자 권한 변경: adminEmail={}, targetUserId={}, newRole={}",
            authentication.getName(), userId, request.role());
        UserSummaryResponse result = adminService.updateUserRole(userId, request.role());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

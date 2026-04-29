package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.request.UpdateRoleRequest;
import com.ssac.ssacbackend.dto.response.UserSummaryResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 사용자 관리 엔드포인트.
 *
 * <p>모든 엔드포인트는 ADMIN 역할이 있어야 접근 가능하다 (SecurityConfig에서 제한).
 */
@Slf4j
@Tag(name = "Admin", description = "관리자 전용 사용자 관리 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(
        summary = "전체 사용자 목록 조회",
        description = """
            [호출 화면] 관리자 대시보드 > 사용자 관리 탭
            [권한 조건] ADMIN 역할 전용.
            [특이 동작] 페이지네이션 지원. 가입일시 내림차순 정렬. 비회원(GUEST)은 목록에 포함되지 않음.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "접근 권한 없음 (ADMIN 전용)")
    })
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

    @Operation(
        summary = "사용자 권한 변경",
        description = """
            [호출 화면] 관리자 대시보드 > 사용자 상세 정보 > 권한 수정 팝업
            [권한 조건] ADMIN 역할 전용.
            [특이 동작] 변경된 권한은 해당 사용자의 다음 API 요청부터 즉시 반영된다. GUEST 역할 부여 불가.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "권한 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "잘못된 역할 또는 유효성 검사 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "접근 권한 없음 (ADMIN 전용)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "사용자를 찾을 수 없음")
    })
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

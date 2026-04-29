package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.MenuClickStatResponse;
import com.ssac.ssacbackend.service.MenuClickEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 메뉴 클릭 통계 엔드포인트.
 *
 * <p>최근 7일 기준 메뉴별 클릭 수 및 CTR을 제공한다.
 * ADMIN 역할만 접근 가능 (SecurityConfig에서 제한).
 */
@Tag(name = "Admin", description = "관리자 전용 사용자 관리 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMenuStatsController {

    private final MenuClickEventService menuClickEventService;

    @Operation(
        summary = "메뉴 클릭 통계 조회",
        description = """
            [호출 화면] 관리자 대시보드 > 메뉴 분석 탭
            [권한 조건] ADMIN 역할 전용.
            [특이 동작] 최근 7일 기준 메뉴별 클릭 수 및 CTR을 반환한다.
            CTR = 메뉴 클릭 수 / 전체 고유 사용자 수 * 100 (소수점 둘째 자리까지).
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "통계 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "접근 권한 없음 (ADMIN 전용)")
    })
    @GetMapping("/menu-stats")
    public ResponseEntity<ApiResponse<List<MenuClickStatResponse>>> getMenuStats() {
        return ResponseEntity.ok(ApiResponse.success(menuClickEventService.getMenuStats()));
    }
}

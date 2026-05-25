package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.ContentMonitoringListResponse;
import com.ssac.ssacbackend.dto.response.ContentSyncResponse;
import com.ssac.ssacbackend.service.NotionSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 콘텐츠 모니터링 및 동기화 API.
 *
 * <p>모든 엔드포인트는 ADMIN 역할이 있어야 접근 가능하다 (SecurityConfig에서 제한).
 * 콘텐츠 편집은 Notion에서 직접 수행하며 BE는 조회·동기화만 담당한다.
 */
@Slf4j
@Tag(name = "Admin Content", description = "관리자 콘텐츠 모니터링 및 Notion 동기화 API")
@RestController
@RequestMapping("/api/v1/admin/contents")
@RequiredArgsConstructor
public class AdminContentController {

    private final NotionSyncService notionSyncService;

    @Operation(
        summary = "콘텐츠 모니터링 목록 조회",
        description = "[권한 조건] ADMIN 역할 전용. Notion 동기화 상태를 모니터링한다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ContentMonitoringListResponse>> getMonitoring(
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(notionSyncService.getMonitoring(page, size)));
    }

    @Operation(
        summary = "Notion 콘텐츠 수동 동기화",
        description = "[권한 조건] ADMIN 역할 전용. Notion 데이터베이스를 즉시 동기화한다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "동기화 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "CONTENT-004: Notion 동기화 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "ADMIN 권한 없음")
    })
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<ContentSyncResponse>> sync(Authentication authentication) {
        log.info("관리자 수동 동기화 요청: admin={}", authentication.getName());
        ContentSyncResponse response = notionSyncService.syncAll();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

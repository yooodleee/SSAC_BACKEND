package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.dto.ErrorLogEntry;
import com.ssac.ssacbackend.service.ErrorLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 로그 진단 API.
 *
 * <p>ADMIN 권한이 필요하며, SecurityConfig에서 /api/v1/admin/** 경로는 ROLE_ADMIN만 접근 가능하다.
 */
@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@Tag(name = "관리자 로그 진단", description = "traceId 기반 에러 로그 조회 API (ADMIN 전용)")
public class AdminLogController {

    private final ErrorLogService errorLogService;

    @GetMapping("/errors")
    @Operation(
        summary = "traceId 기반 에러 로그 조회",
        description = "특정 traceId의 전체 에러 로그를 시간 순으로 반환한다. ADMIN 권한 필요."
    )
    public TraceLogResponse getErrorsByTraceId(
        @Parameter(description = "추적할 traceId (X-Trace-Id 응답 헤더 값)")
        @RequestParam String traceId
    ) {
        List<ErrorLogEntry> entries = errorLogService.findByTraceId(traceId);
        return new TraceLogResponse(traceId, entries);
    }

    public record TraceLogResponse(String traceId, List<ErrorLogEntry> logs) {}
}

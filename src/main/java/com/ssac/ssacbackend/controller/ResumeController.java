package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.request.UpdateProgressRequest;
import com.ssac.ssacbackend.dto.response.ResumeContentResponse;
import com.ssac.ssacbackend.dto.response.ResumeResponse;
import com.ssac.ssacbackend.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이어보기 API.
 *
 * <p>인증된 사용자만 접근 가능하다.
 */
@Slf4j
@Tag(name = "Resume", description = "이어보기 API")
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(
        summary = "이어보기 조회",
        description = "가장 최근에 학습한 미완료 콘텐츠를 반환한다. 없으면 hasResume: false.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<ApiResponse<ResumeResponse>> getResume(Authentication authentication) {
        log.debug("이어보기 조회: email={}", authentication.getName());
        ResumeResponse result = resumeService.getResume(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
        summary = "진행 상황 갱신",
        description = "콘텐츠의 lastPosition과 progressRate를 갱신한다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeContentResponse>> updateProgress(
        @PathVariable Long id,
        @RequestBody @Valid UpdateProgressRequest request,
        Authentication authentication) {
        log.debug("진행 상황 갱신: id={}, email={}", id, authentication.getName());
        ResumeContentResponse result = resumeService.updateProgress(
            id, authentication.getName(), request.lastPosition(), request.progressRate());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

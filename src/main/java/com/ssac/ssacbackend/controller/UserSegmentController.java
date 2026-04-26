package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.UserSegmentResponse;
import com.ssac.ssacbackend.service.UserSegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 세그먼트 API.
 *
 * <p>인증된 사용자만 접근 가능하다.
 * 세그먼트 기준: 완료 콘텐츠 수 5개 미만 → beginner, 5개 이상 → advanced.
 * 신규 사용자(데이터 없음)는 기본값 beginner를 반환한다.
 */
@Slf4j
@Tag(name = "UserSegment", description = "사용자 세그먼트 API")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserSegmentController {

    private final UserSegmentService userSegmentService;

    @Operation(
        summary = "사용자 세그먼트 조회",
        description = "완료 콘텐츠 수 기준으로 beginner 또는 advanced를 반환한다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/segment")
    public ResponseEntity<ApiResponse<UserSegmentResponse>> getSegment(
        Authentication authentication) {
        log.debug("세그먼트 조회: email={}", authentication.getName());
        UserSegmentResponse result = userSegmentService.getSegment(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * 홈 화면 API 엔드포인트.
 *
 * <p>모든 요청은 USER 권한의 JWT가 필요하다.
 */
@Slf4j
@Tag(name = "Home", description = "홈 화면 API")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(
        summary = "홈 화면 조회",
        description = """
            [호출 화면] 앱 진입 시 홈 화면 데이터 조회.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] 온보딩 미완료 시 onboardingRequired: true와 redirectTo 반환.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "홈 화면 데이터 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getHome(Authentication authentication) {
        log.debug("홈 화면 조회: email={}", authentication.getName());
        Object result = homeService.getHome(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

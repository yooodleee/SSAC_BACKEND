package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.request.OnboardingSubmitRequest;
import com.ssac.ssacbackend.dto.response.OnboardingQuestionsResponse;
import com.ssac.ssacbackend.dto.response.OnboardingSkipResponse;
import com.ssac.ssacbackend.dto.response.OnboardingSubmitResponse;
import com.ssac.ssacbackend.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 온보딩 테스트 문제 조회 및 결과 제출 엔드포인트.
 *
 * <p>모든 요청은 USER 권한의 JWT가 필요하다.
 */
@Slf4j
@Tag(name = "Onboarding", description = "온보딩 테스트 API")
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(
        summary = "온보딩 문제 조회",
        description = """
            [호출 화면] 온보딩 테스트 시작 시 호출.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] userType 미설정 시 400, 이미 완료한 경우 409 반환.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "문제 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "ONBOARDING-001: userType 미설정"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "ONBOARDING-002: 이미 온보딩 완료")
    })
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<OnboardingQuestionsResponse>> getQuestions(
        Authentication authentication) {
        log.debug("온보딩 문제 조회 요청: email={}", authentication.getName());
        OnboardingQuestionsResponse response =
            onboardingService.getQuestions(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "온보딩 응답 제출",
        description = """
            [호출 화면] 온보딩 테스트 완료 후 제출 시 호출.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] 5개 응답 필수. 총점 기준으로 레벨 자동 판정(SEED/SPROUT/TREE).
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "레벨 판정 및 저장 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "ONBOARDING-003: 응답 수 오류 / ONBOARDING-004: 유효하지 않은 문제 / "
                + "ONBOARDING-005: 유형 불일치"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "ONBOARDING-002: 이미 온보딩 완료")
    })
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<OnboardingSubmitResponse>> submit(
        Authentication authentication,
        @RequestBody @Valid OnboardingSubmitRequest request) {
        log.debug("온보딩 응답 제출 요청: email={}", authentication.getName());
        OnboardingSubmitResponse response =
            onboardingService.submit(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "온보딩 건너뛰기",
        description = """
            [호출 화면] 온보딩 테스트 건너뛰기 선택 시 호출.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] 레벨을 기본값 SEED로 설정하고 온보딩 완료 처리한다.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "SEED 레벨 설정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자")
    })
    @PostMapping("/skip")
    public ResponseEntity<ApiResponse<OnboardingSkipResponse>> skip(
        Authentication authentication) {
        log.debug("온보딩 건너뛰기 요청: email={}", authentication.getName());
        OnboardingSkipResponse response = onboardingService.skip(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

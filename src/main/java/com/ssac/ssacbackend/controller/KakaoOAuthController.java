package com.ssac.ssacbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.lang.Nullable;

/**
 * 카카오 OAuth2 로그인 엔드포인트 Swagger 문서화용 더미 컨트롤러.
 *
 * <p>실제 요청은 Spring Security OAuth2 필터 체인이 처리하므로
 * 이 컨트롤러의 메서드는 호출되지 않는다. Swagger 노출 전용이다.
 */
@RestController
@RequestMapping
@Tag(name = "카카오 OAuth", description = "카카오 소셜 로그인 API")
public class KakaoOAuthController {

    /**
     * 카카오 OAuth 인증 페이지로 리다이렉트한다.
     *
     * <p>실제 처리는 Spring Security OAuth2 필터가 담당한다.
     */
    @GetMapping("/oauth2/authorization/kakao")
    @Operation(
        summary = "카카오 로그인 시작",
        description = "카카오 OAuth 인증 페이지로 리다이렉트한다. "
            + "redirectTo 파라미터를 전달하면 인증 후 해당 경로로 돌아간다(상대 경로만 허용). "
            + "실제 요청은 Spring Security OAuth2 필터가 처리한다."
    )
    public void login(
        @Parameter(description = "인증 후 돌아갈 경로 (예: /my-page). 생략 시 /로 이동.")
        @RequestParam(required = false) @Nullable String redirectTo
    ) {
        throw new UnsupportedOperationException("Spring Security OAuth2 필터가 처리합니다.");
    }

    /**
     * 카카오 인증 코드를 JWT 토큰으로 교환한다.
     *
     * <p>실제 처리는 Spring Security OAuth2 필터 → OAuth2SuccessHandler가 담당한다.
     * 로그인 성공 시 Access Token과 Refresh Token이 HttpOnly 쿠키로 전달된다.
     *
     * @param code  카카오가 전달한 인증 코드
     * @param state CSRF 방어용 state 파라미터
     */
    @GetMapping("/login/oauth2/code/kakao")
    @Operation(
        summary = "카카오 로그인 콜백",
        description = "카카오 인증 코드를 받아 Access Token과 Refresh Token을 발급한다. "
            + "두 토큰 모두 HttpOnly 쿠키로 전달된다. 신규 사용자는 자동 가입된다. "
            + "guestId 쿠키가 있으면 비회원 퀴즈 기록을 회원 계정으로 자동 이전한다. "
            + "실제 요청은 Spring Security OAuth2 필터가 처리한다."
    )
    public void callback(
        @Parameter(description = "카카오가 전달한 인증 코드") @RequestParam String code,
        @Parameter(description = "CSRF 방어용 state 파라미터") @RequestParam String state
    ) {
        throw new UnsupportedOperationException("Spring Security OAuth2 필터가 처리합니다.");
    }
}

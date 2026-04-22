package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.response.LoginResponse;
import com.ssac.ssacbackend.service.NaverOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 네이버 OAuth 2.0 로그인 컨트롤러.
 *
 * <p>/api/v1/auth/naver 하위에 인증 시작 및 콜백 엔드포인트를 제공한다.
 * 두 경로 모두 SecurityConfig에 의해 인증 없이 접근 가능하다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/naver")
@RequiredArgsConstructor
@Tag(name = "네이버 OAuth", description = "네이버 소셜 로그인 API")
public class NaverOAuthController {

    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7일

    private final NaverOAuthService naverOAuthService;

    /**
     * 네이버 OAuth 인증 페이지로 리다이렉트한다.
     *
     * <p>CSRF 방어를 위한 state 파라미터를 생성하고 네이버 인증 URL로 리다이렉트한다.
     */
    @GetMapping("/login")
    @Operation(
        summary = "네이버 로그인 시작",
        description = "네이버 OAuth 인증 페이지로 리다이렉트한다. state 파라미터로 CSRF를 방어한다."
    )
    public void login(HttpServletResponse response) throws IOException {
        String authorizationUrl = naverOAuthService.generateAuthorizationUrl();
        log.debug("네이버 로그인 리다이렉트: url={}", authorizationUrl);
        response.sendRedirect(authorizationUrl);
    }

    /**
     * 네이버 인증 코드를 JWT 토큰으로 교환한다.
     *
     * <p>state 검증 후 네이버 API에서 사용자 정보를 조회하고, 서비스 내부 사용자로 매핑하여
     * Access Token을 발급한다. Refresh Token은 HttpOnly 쿠키로 전달된다.
     * 신규 사용자라면 자동으로 회원 가입이 진행된다.
     *
     * @param code  네이버가 전달한 인증 코드
     * @param state CSRF 방어용 state 파라미터
     * @return 발급된 Access Token
     */
    @GetMapping("/callback")
    @Operation(
        summary = "네이버 로그인 콜백",
        description = "네이버 인증 코드를 받아 Access Token을 발급한다. "
            + "Refresh Token은 HttpOnly 쿠키로 전달된다. 신규 사용자는 자동 가입된다."
    )
    public ResponseEntity<ApiResponse<LoginResponse>> callback(
        @Parameter(description = "네이버가 전달한 인증 코드") @RequestParam String code,
        @Parameter(description = "CSRF 방어용 state 파라미터") @RequestParam String state,
        HttpServletResponse response
    ) {
        log.debug("네이버 콜백 수신: state={}", state);
        TokenPair tokens = naverOAuthService.processCallback(code, state);

        Cookie refreshTokenCookie = new Cookie("refreshToken", tokens.refreshToken());
        refreshTokenCookie.setPath("/api/v1/auth");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        // refreshTokenCookie.setSecure(true); // 운영 환경(HTTPS)에서 활성화
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(
            ApiResponse.success(new LoginResponse(tokens.accessToken(), "Bearer"))
        );
    }
}

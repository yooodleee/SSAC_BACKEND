package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.util.CookieUtils;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.NaverLoginResult;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.service.NaverOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CookieValue;
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

    private final NaverOAuthService naverOAuthService;
    private final CookieProperties cookieProperties;

    @Value("${oauth2.default-redirect-uri:http://localhost:3000}")
    private String defaultRedirectUri;

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
     * Access Token / Refresh Token을 HttpOnly 쿠키에 저장한 뒤 프론트엔드로 리다이렉트한다.
     * 신규 사용자라면 자동으로 회원 가입이 진행된다.
     *
     * @param code  네이버가 전달한 인증 코드
     * @param state CSRF 방어용 state 파라미터
     */
    @GetMapping("/callback")
    @Operation(
        summary = "네이버 로그인 콜백",
        description = "네이버 인증 코드를 처리한다. "
            + "기존 회원이면 Access/Refresh Token을 HttpOnly 쿠키로 저장 후 프론트엔드로 리다이렉트한다. "
            + "신규 회원이면 isNewUser=true와 tempToken 파라미터와 함께 회원 가입 플로우로 리다이렉트한다. "
            + "guestId 쿠키가 있으면 기존 회원의 경우 비회원 퀴즈 기록을 자동 이전한다."
    )
    public void callback(
        @Parameter(description = "네이버가 전달한 인증 코드") @RequestParam String code,
        @Parameter(description = "CSRF 방어용 state 파라미터") @RequestParam String state,
        @CookieValue(name = "guestId", required = false) String guestId,
        HttpServletResponse response
    ) throws IOException {
        log.debug("네이버 콜백 수신: state={}, guestId={}", state, guestId);
        NaverLoginResult result = naverOAuthService.processCallback(code, state, guestId);

        if (result.isNewUser()) {
            // 신규 회원: 회원 가입 플로우로 리다이렉트
            log.info("네이버 신규 회원 리다이렉트: tempToken 발급 완료");
            response.sendRedirect(defaultRedirectUri
                + "/auth/naver/callback?isNewUser=true&tempToken=" + result.tempToken()
                + "&provider=NAVER");
        } else {
            // 기존 회원: 토큰 쿠키 설정 후 리다이렉트
            TokenPair tokens = result.tokenPair();
            CookieUtils.addAccessTokenCookie(response, tokens.accessToken(), cookieProperties);
            CookieUtils.addRefreshTokenCookie(response, tokens.refreshToken(), cookieProperties);
            if (guestId != null) {
                CookieUtils.clearGuestIdCookie(response, cookieProperties);
                log.debug("네이버 로그인 후 guestId 쿠키 삭제: guestId={}", guestId);
            }
            log.info("네이버 기존 회원 로그인 성공: 토큰 발급 완료, redirectUri={}", defaultRedirectUri);
            response.sendRedirect(defaultRedirectUri + "?isNewUser=false");
        }
    }
}

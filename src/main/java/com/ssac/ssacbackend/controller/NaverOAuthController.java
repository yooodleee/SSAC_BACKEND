package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.util.CookieUtils;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.NaverLoginResult;
import com.ssac.ssacbackend.service.AuthCodeService;
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
 *
 * <p>콜백 처리 후 JWT/tempToken을 리다이렉트 URL에 직접 노출하지 않는다.
 * 30초 TTL 일회용 authCode를 발급하고, FE가 {@code POST /api/v1/auth/token}으로 교환한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/naver")
@RequiredArgsConstructor
@Tag(name = "네이버 OAuth", description = "네이버 소셜 로그인 API")
public class NaverOAuthController {

    private final NaverOAuthService naverOAuthService;
    private final AuthCodeService authCodeService;
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
        try {
            String authorizationUrl = naverOAuthService.generateAuthorizationUrl();
            log.debug("네이버 로그인 리다이렉트: url={}", authorizationUrl);
            response.sendRedirect(authorizationUrl);
        } catch (Exception e) {
            log.error("네이버 로그인 URL 생성 실패: {}", e.getMessage());
            response.sendRedirect(defaultRedirectUri + "/auth/naver/callback?loginError=server_error");
        }
    }

    /**
     * 네이버 인증 코드를 처리하고 일회용 authCode를 발급해 프론트엔드로 리다이렉트한다.
     *
     * <p>state 검증 후 네이버 API에서 사용자 정보를 조회하고, 서비스 내부 사용자로 매핑하여
     * 30초 TTL 일회용 authCode를 발급한다. JWT/tempToken을 URL에 직접 노출하지 않는다.
     * FE는 authCode를 {@code POST /api/v1/auth/token}으로 교환해 실제 토큰을 받는다.
     * 네이버가 error 파라미터와 함께 콜백하는 경우(사용자 거부, 보안 검증 등)
     * 프론트엔드 에러 페이지로 리다이렉트한다.
     *
     * @param code             네이버가 전달한 인증 코드 (에러 응답 시 absent)
     * @param state            CSRF 방어용 state 파라미터
     * @param error            네이버 에러 코드 (정상 응답 시 absent)
     * @param errorDescription 네이버 에러 설명 (정상 응답 시 absent)
     */
    @GetMapping("/callback")
    @Operation(
        summary = "네이버 로그인 콜백",
        description = "네이버 인증 코드를 처리한다. "
            + "기존 회원이면 authCode와 isNewUser=false로 리다이렉트한다. "
            + "신규 회원이면 authCode와 isNewUser=true로 회원 가입 플로우로 리다이렉트한다. "
            + "JWT/tempToken은 URL에 노출되지 않으며 FE가 POST /api/v1/auth/token으로 교환한다. "
            + "guestId 쿠키가 있으면 기존 회원의 비회원 퀴즈 기록을 자동 이전한다. "
            + "네이버가 error 파라미터를 전달하는 경우 FE 에러 페이지로 리다이렉트한다."
    )
    public void callback(
        @Parameter(description = "네이버가 전달한 인증 코드") @RequestParam(required = false) String code,
        @Parameter(description = "CSRF 방어용 state 파라미터") @RequestParam(required = false) String state,
        @Parameter(description = "네이버 에러 코드 (사용자 거부 등)") @RequestParam(required = false) String error,
        @Parameter(description = "네이버 에러 설명") @RequestParam(name = "error_description", required = false) String errorDescription,
        @CookieValue(name = "guestId", required = false) String guestId,
        HttpServletResponse response
    ) throws IOException {
        // 네이버가 에러를 반환한 경우: access_denied (사용자 거부), 보안 검증 실패 등
        if (error != null) {
            log.warn("네이버 OAuth 에러 콜백: error={}, description={}", error, errorDescription);
            response.sendRedirect(defaultRedirectUri + "/auth/naver/callback?loginError=" + error);
            return;
        }

        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            log.warn("네이버 콜백: 필수 파라미터 누락 (code={}, state={})", code, state);
            response.sendRedirect(defaultRedirectUri + "/auth/naver/callback?loginError=invalid_request");
            return;
        }

        log.debug("네이버 콜백 수신: state={}, guestId={}", state, guestId);
        NaverLoginResult result;
        try {
            result = naverOAuthService.processCallback(code, state, guestId);
        } catch (Exception e) {
            log.error("네이버 로그인 처리 중 오류 발생: {}", e.getMessage());
            response.sendRedirect(defaultRedirectUri + "/auth/naver/callback?loginError=server_error");
            return;
        }

        if (result.isNewUser()) {
            // 신규 회원: tempToken을 authCode로 감싸 리다이렉트 (tempToken URL 노출 없음)
            String authCode = authCodeService.issueForNewUser(result.tempToken(), "NAVER");
            log.info("네이버 신규 회원 리다이렉트: authCode 발급 완료");
            response.sendRedirect(defaultRedirectUri
                + "/auth/naver/callback?authCode=" + authCode + "&isNewUser=true");
        } else {
            // 기존 회원: userId로 authCode 발급 후 리다이렉트 (JWT URL 노출 없음, 쿠키 불필요)
            String authCode = authCodeService.issueForExistingUser(result.userId());
            if (guestId != null) {
                CookieUtils.clearGuestIdCookie(response, cookieProperties);
                log.debug("네이버 로그인 후 guestId 쿠키 삭제: guestId={}", guestId);
            }
            log.info("네이버 기존 회원 로그인 성공: authCode 발급 완료");
            response.sendRedirect(defaultRedirectUri
                + "/auth/naver/callback?authCode=" + authCode + "&isNewUser=false");
        }
    }
}

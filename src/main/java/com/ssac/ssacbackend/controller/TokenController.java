package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.response.LoginResponse;
import com.ssac.ssacbackend.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Access Token 재발급 및 로그아웃 컨트롤러.
 *
 * <p>/api/v1/auth 하위에 reissue, logout 엔드포인트를 제공한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "토큰 관리", description = "Access Token 재발급 및 로그아웃 API")
public class TokenController {

    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7일

    private final TokenService tokenService;

    /**
     * Refresh Token으로 새 Access Token과 Refresh Token을 재발급한다(Rotation).
     *
     * <p>Refresh Token은 refreshToken 쿠키에서 읽는다.
     * 재발급 후 기존 Refresh Token은 무효화되고, 새 Refresh Token이 쿠키에 설정된다.
     */
    @PostMapping("/reissue")
    @Operation(
        summary = "토큰 재발급",
        description = "Refresh Token(쿠키)으로 새 Access Token을 발급한다. Refresh Token도 Rotation된다."
    )
    public ResponseEntity<ApiResponse<LoginResponse>> reissue(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw BusinessException.badRequest("Refresh Token이 없습니다.");
        }

        TokenPair tokens = tokenService.reissue(refreshToken);
        setRefreshTokenCookie(response, tokens.refreshToken());

        return ResponseEntity.ok(
            ApiResponse.success(new LoginResponse(tokens.accessToken(), "Bearer"))
        );
    }

    /**
     * Refresh Token을 무효화하고 쿠키를 삭제하여 로그아웃 처리한다.
     */
    @PostMapping("/logout")
    @Operation(
        summary = "로그아웃",
        description = "Refresh Token을 무효화하고 관련 쿠키를 삭제한다."
    )
    public ResponseEntity<ApiResponse<Void>> logout(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshToken != null) {
            tokenService.logout(refreshToken);
        }
        clearCookie(response, "refreshToken", "/api/v1/auth");
        clearCookie(response, "accessToken", "/");
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setPath("/api/v1/auth");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        // cookie.setSecure(true); // 운영 환경(HTTPS)에서 활성화
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}

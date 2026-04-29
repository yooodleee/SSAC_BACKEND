package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.common.util.CookieUtils;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.response.LoginResponse;
import com.ssac.ssacbackend.service.JwtService;
import com.ssac.ssacbackend.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
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

    private final TokenService tokenService;
    private final JwtService jwtService;
    private final CookieProperties cookieProperties;

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
        CookieUtils.addRefreshTokenCookie(response, tokens.refreshToken(), cookieProperties);

        return ResponseEntity.ok(
            ApiResponse.success(new LoginResponse(tokens.accessToken(), "Bearer"))
        );
    }

    /**
     * 비회원(Guest)에게 임시 JWT를 발급한다.
     *
     * <p>UUID 기반 guestId를 생성하고 GUEST role의 Access Token을 쿠키로 전달한다.
     * guestId 쿠키(30일)를 별도로 설정하여 로그인 시 데이터 마이그레이션에 활용한다.
     */
    @PostMapping("/guest")
    @Operation(
        summary = "비회원 토큰 발급",
        description = "로그인 없이 서비스를 이용할 수 있는 임시 Guest 토큰을 발급한다. "
            + "기존 guestId 쿠키가 있다면 재사용하고, 없다면 새로 생성한다."
    )
    public ResponseEntity<ApiResponse<LoginResponse>> issueGuestToken(
        @CookieValue(name = "guestId", required = false) String existingGuestId,
        HttpServletResponse response
    ) {
        String guestId = (existingGuestId != null && !existingGuestId.isBlank())
            ? existingGuestId
            : UUID.randomUUID().toString();

        String accessToken = jwtService.generateGuestToken(guestId);

        CookieUtils.addAccessTokenCookie(response, accessToken, cookieProperties);
        CookieUtils.addGuestIdCookie(response, guestId, cookieProperties);

        log.info("Guest 토큰 발급: guestId={}, reused={}", guestId, existingGuestId != null);
        return ResponseEntity.ok(ApiResponse.success(new LoginResponse(accessToken, "Bearer")));
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
        CookieUtils.clearRefreshTokenCookie(response, cookieProperties);
        CookieUtils.clearAccessTokenCookie(response, cookieProperties);
        CookieUtils.clearGuestIdCookie(response, cookieProperties);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}

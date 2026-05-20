package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.common.util.CookieUtils;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.request.AdminLoginRequest;
import com.ssac.ssacbackend.dto.response.AdminLoginResponse;
import com.ssac.ssacbackend.service.AdminLoginService;
import com.ssac.ssacbackend.service.AdminLoginService.LoginResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 코드 로그인 API.
 *
 * <p>사전 발급된 관리자 코드로 로그인하고 JWT를 발급한다.
 * 인증 불필요 — SecurityConfig의 /api/v1/auth/** PUBLIC 정책에 포함된다.
 */
@Tag(name = "Admin Auth", description = "관리자 인증 API")
@RestController
@RequestMapping("/api/v1/auth/admin")
@RequiredArgsConstructor
public class AdminLoginController {

    private final AdminLoginService adminLoginService;
    private final CookieProperties cookieProperties;

    @Operation(
        summary = "관리자 코드 로그인",
        description = "사전 발급된 관리자 코드로 로그인한다. 코드는 1회 사용 후 재사용 불가. "
            + "성공 시 Refresh Token은 HttpOnly Cookie로 설정된다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(
        @RequestBody AdminLoginRequest request,
        HttpServletResponse httpResponse
    ) {
        LoginResult result = adminLoginService.login(request.adminCode());
        CookieUtils.addRefreshTokenCookie(httpResponse, result.refreshToken(), cookieProperties);
        return ResponseEntity.ok(ApiResponse.success(result.response()));
    }
}

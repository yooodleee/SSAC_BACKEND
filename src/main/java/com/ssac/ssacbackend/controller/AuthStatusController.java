package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.AuthStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 상태 확인 API.
 *
 * <p>비로그인 사용자도 접근 가능하다 (PUBLIC 경로).
 * 인증 여부에 따라 isAuthenticated, role, redirectTo를 반환한다.
 */
@Tag(name = "Auth", description = "인증 상태 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthStatusController {

    @Operation(
        summary = "인증 상태 확인",
        description = "현재 요청의 인증 상태를 반환한다. 비로그인 시 isAuthenticated: false."
    )
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AuthStatusResponse>> getStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(ApiResponse.success(AuthStatusResponse.unauthenticated()));
        }

        String role = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.substring(5))
            .findFirst()
            .orElse("USER");

        return ResponseEntity.ok(ApiResponse.success(AuthStatusResponse.authenticated(role)));
    }
}

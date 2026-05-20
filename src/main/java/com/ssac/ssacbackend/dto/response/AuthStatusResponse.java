package com.ssac.ssacbackend.dto.response;

/**
 * 인증 상태 응답 DTO (GET /api/v1/auth/status).
 */
public record AuthStatusResponse(
    boolean isAuthenticated,
    String role,
    String redirectTo
) {
    public static AuthStatusResponse unauthenticated() {
        return new AuthStatusResponse(false, null, null);
    }

    public static AuthStatusResponse authenticated(String role) {
        String redirectTo = "ADMIN".equals(role) ? "/admin" : "/home";
        return new AuthStatusResponse(true, role, redirectTo);
    }
}

package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.request.AdminLoginRequest;
import com.ssac.ssacbackend.dto.response.AdminLoginResponse;
import com.ssac.ssacbackend.service.AdminLoginService;
import com.ssac.ssacbackend.service.AdminLoginService.LoginResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("AdminLoginController")
class AdminLoginControllerTest {

    private AdminLoginService adminLoginService;
    private AdminLoginController controller;

    @BeforeEach
    void setUp() {
        adminLoginService = mock(AdminLoginService.class);
        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");
        controller = new AdminLoginController(adminLoginService, cookieProperties);
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login - 관리자 코드 로그인")
    class Login {

        @Test
        @DisplayName("유효한 관리자 코드로 로그인 성공 시 200을 반환한다")
        void login_성공() {
            AdminLoginRequest request = new AdminLoginRequest("valid-admin-code");
            AdminLoginResponse mockResponse = new AdminLoginResponse(
                "access-token", "Bearer", 3600L,
                new AdminLoginResponse.UserInfo("1", "관리자", "ADMIN", "/admin/dashboard")
            );
            LoginResult mockResult = new LoginResult(mockResponse, "refresh-token");
            given(adminLoginService.login("valid-admin-code")).willReturn(mockResult);
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            ResponseEntity<ApiResponse<AdminLoginResponse>> result =
                controller.login(request, httpResponse);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(mockResponse);
        }

        @Test
        @DisplayName("로그인 성공 시 Refresh Token이 HttpOnly 쿠키로 설정된다")
        void login_쿠키설정() {
            AdminLoginRequest request = new AdminLoginRequest("admin-code");
            AdminLoginResponse mockResponse = new AdminLoginResponse(
                "access-token", "Bearer", 3600L,
                new AdminLoginResponse.UserInfo("1", "관리자", "ADMIN", "/admin")
            );
            LoginResult mockResult = new LoginResult(mockResponse, "admin-refresh-token");
            given(adminLoginService.login("admin-code")).willReturn(mockResult);
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            controller.login(request, httpResponse);

            assertThat(httpResponse.getHeaders("Set-Cookie"))
                .anyMatch(h -> h.contains("admin-refresh-token"));
        }

        @Test
        @DisplayName("adminCode를 서비스에 그대로 전달한다")
        void login_서비스호출() {
            AdminLoginRequest request = new AdminLoginRequest("test-code");
            AdminLoginResponse mockResponse = new AdminLoginResponse(
                "token", "Bearer", 3600L,
                new AdminLoginResponse.UserInfo("1", "admin", "ADMIN", "/admin")
            );
            given(adminLoginService.login("test-code"))
                .willReturn(new LoginResult(mockResponse, "rt"));
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            controller.login(request, httpResponse);

            verify(adminLoginService).login("test-code");
        }
    }
}

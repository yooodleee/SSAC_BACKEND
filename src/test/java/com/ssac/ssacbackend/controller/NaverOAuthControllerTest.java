package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.NaverLoginResult;
import com.ssac.ssacbackend.service.AuthCodeService;
import com.ssac.ssacbackend.service.NaverOAuthService;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("NaverOAuthController")
class NaverOAuthControllerTest {

    private NaverOAuthService naverOAuthService;
    private AuthCodeService authCodeService;
    private NaverOAuthController controller;

    @BeforeEach
    void setUp() {
        naverOAuthService = mock(NaverOAuthService.class);
        authCodeService = mock(AuthCodeService.class);

        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");

        controller = new NaverOAuthController(naverOAuthService, authCodeService, cookieProperties);
        ReflectionTestUtils.setField(controller, "defaultRedirectUri", "http://localhost:3000");
    }

    // ── 에러 케이스 ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("에러 파라미터 처리")
    class ErrorParam {

        @Test
        @DisplayName("네이버가 error 파라미터를 전달하면 loginError로 리다이렉트한다")
        void redirectsOnNaverError() throws IOException {
            MockHttpServletResponse response = new MockHttpServletResponse();

            controller.callback("code", "state", "access_denied", "사용자 거부", null, response);

            assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/auth/naver/callback?loginError=access_denied");
            verify(naverOAuthService, never()).processCallback(any(), any(), any());
        }

        @Test
        @DisplayName("code가 없으면 invalid_request로 리다이렉트한다")
        void redirectsOnMissingCode() throws IOException {
            MockHttpServletResponse response = new MockHttpServletResponse();

            controller.callback(null, "state", null, null, null, response);

            assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/auth/naver/callback?loginError=invalid_request");
        }

        @Test
        @DisplayName("processCallback이 예외를 던지면 server_error로 리다이렉트한다")
        void redirectsOnServiceException() throws IOException {
            given(naverOAuthService.processCallback(any(), any(), any()))
                .willThrow(new RuntimeException("네이버 API 오류"));
            MockHttpServletResponse response = new MockHttpServletResponse();

            controller.callback("code", "state", null, null, null, response);

            assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/auth/naver/callback?loginError=server_error");
        }
    }

    // ── 기존 회원 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("기존 회원 콜백")
    class ExistingUser {

        @Test
        @DisplayName("authCode와 isNewUser=false를 담아 리다이렉트한다 (JWT URL 노출 없음)")
        void redirectsWithAuthCodeNotJwt() throws IOException {
            given(naverOAuthService.processCallback(eq("code"), eq("state"), eq(null)))
                .willReturn(NaverLoginResult.existingUser(10L));
            given(authCodeService.issueForExistingUser(10L)).willReturn("auth-code-existing");

            MockHttpServletResponse response = new MockHttpServletResponse();
            controller.callback("code", "state", null, null, null, response);

            assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/auth/naver/callback?authCode=auth-code-existing&isNewUser=false");
            assertThat(response.getRedirectedUrl())
                .doesNotContain("token=")
                .doesNotContain("accessToken");
        }

        @Test
        @DisplayName("guestId 쿠키가 있으면 쿠키를 삭제한다")
        void clearsGuestIdCookieWhenPresent() throws IOException {
            given(naverOAuthService.processCallback(eq("code"), eq("state"), eq("guest-uuid")))
                .willReturn(NaverLoginResult.existingUser(10L));
            given(authCodeService.issueForExistingUser(10L)).willReturn("auth-code-existing");

            MockHttpServletResponse response = new MockHttpServletResponse();
            controller.callback("code", "state", null, null, "guest-uuid", response);

            List<String> cookies = response.getHeaders("Set-Cookie");
            assertThat(cookies).anyMatch(h -> h.startsWith("guestId=") && h.contains("Max-Age=0"));
        }

        @Test
        @DisplayName("guestId 쿠키가 없으면 쿠키를 삭제하지 않는다")
        void doesNotClearGuestIdCookieWhenAbsent() throws IOException {
            given(naverOAuthService.processCallback(eq("code"), eq("state"), eq(null)))
                .willReturn(NaverLoginResult.existingUser(10L));
            given(authCodeService.issueForExistingUser(10L)).willReturn("auth-code-existing");

            MockHttpServletResponse response = new MockHttpServletResponse();
            controller.callback("code", "state", null, null, null, response);

            List<String> cookies = response.getHeaders("Set-Cookie");
            assertThat(cookies).noneMatch(h -> h.startsWith("guestId="));
        }
    }

    // ── 신규 회원 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("신규 회원 콜백")
    class NewUser {

        @Test
        @DisplayName("authCode와 isNewUser=true를 담아 리다이렉트한다 (tempToken URL 노출 없음)")
        void redirectsWithAuthCodeNotTempToken() throws IOException {
            given(naverOAuthService.processCallback(eq("code"), eq("state"), eq(null)))
                .willReturn(NaverLoginResult.newUser("my-temp-token"));
            given(authCodeService.issueForNewUser("my-temp-token", "NAVER"))
                .willReturn("auth-code-new");

            MockHttpServletResponse response = new MockHttpServletResponse();
            controller.callback("code", "state", null, null, null, response);

            assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/auth/naver/callback?authCode=auth-code-new&isNewUser=true");
            assertThat(response.getRedirectedUrl())
                .doesNotContain("tempToken")
                .doesNotContain("token=");
        }
    }
}

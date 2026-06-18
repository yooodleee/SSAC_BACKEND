package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.RegisterV2Result;
import com.ssac.ssacbackend.dto.request.EmailLoginRequest;
import com.ssac.ssacbackend.dto.request.EmailRegisterRequest;
import com.ssac.ssacbackend.dto.request.RegisterV2Request;
import com.ssac.ssacbackend.dto.response.EmailCheckResponse;
import com.ssac.ssacbackend.dto.response.RegisterV2Response;
import com.ssac.ssacbackend.service.EmailAuthService;
import com.ssac.ssacbackend.service.RegistrationV2Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("AuthV1Controller")
class AuthV1ControllerTest {

    private RegistrationV2Service registrationV2Service;
    private EmailAuthService emailAuthService;
    private AuthV1Controller controller;

    @BeforeEach
    void setUp() {
        registrationV2Service = mock(RegistrationV2Service.class);
        emailAuthService = mock(EmailAuthService.class);
        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");
        controller = new AuthV1Controller(registrationV2Service, emailAuthService, cookieProperties);
    }

    // ── register ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register - 소셜 회원 가입")
    class Register {

        @Test
        @DisplayName("회원 가입 성공 시 200과 RegisterV2Response를 반환한다")
        void register_성공() {
            RegisterV2Request request = mock(RegisterV2Request.class);
            RegisterV2Response mockResponse = mockRegisterV2Response();
            RegisterV2Result mockResult = new RegisterV2Result("refresh-token", mockResponse);
            given(registrationV2Service.registerV2(any())).willReturn(mockResult);
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            ResponseEntity<?> result = controller.register(request, httpResponse);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(registrationV2Service).registerV2(request);
        }

        @Test
        @DisplayName("회원 가입 성공 시 Refresh Token이 쿠키로 설정된다")
        void register_리프레시토큰_쿠키설정() {
            RegisterV2Request request = mock(RegisterV2Request.class);
            RegisterV2Response mockResponse = mockRegisterV2Response();
            RegisterV2Result mockResult = new RegisterV2Result("my-refresh-token", mockResponse);
            given(registrationV2Service.registerV2(any())).willReturn(mockResult);
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            controller.register(request, httpResponse);

            assertThat(httpResponse.getHeaders("Set-Cookie"))
                .anyMatch(h -> h.contains("my-refresh-token"));
        }
    }

    // ── registerWithEmail ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerWithEmail - 이메일+비밀번호 회원 가입")
    class RegisterWithEmail {

        @Test
        @DisplayName("이메일 회원 가입 성공 시 200을 반환한다")
        void registerWithEmail_성공() {
            EmailRegisterRequest request = mock(EmailRegisterRequest.class);
            RegisterV2Response mockResponse = mockRegisterV2Response();
            RegisterV2Result mockResult = new RegisterV2Result("refresh-token", mockResponse);
            given(registrationV2Service.registerWithEmail(any())).willReturn(mockResult);
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            ResponseEntity<?> result = controller.registerWithEmail(request, httpResponse);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(registrationV2Service).registerWithEmail(request);
        }
    }

    // ── loginWithEmail ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("loginWithEmail - 이메일+비밀번호 로그인")
    class LoginWithEmail {

        @Test
        @DisplayName("이메일 로그인 성공 시 200을 반환한다")
        void loginWithEmail_성공() {
            EmailLoginRequest request = new EmailLoginRequest("user@test.com", "password123");
            RegisterV2Response mockResponse = mockRegisterV2Response();
            RegisterV2Result mockResult = new RegisterV2Result("refresh-token", mockResponse);
            given(emailAuthService.loginWithEmail(any())).willReturn(mockResult);
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            ResponseEntity<?> result = controller.loginWithEmail(request, httpResponse);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(emailAuthService).loginWithEmail(request);
        }

        @Test
        @DisplayName("이메일 로그인 성공 시 Refresh Token이 쿠키로 설정된다")
        void loginWithEmail_쿠키설정() {
            EmailLoginRequest request = new EmailLoginRequest("user@test.com", "password123");
            RegisterV2Response mockResponse = mockRegisterV2Response();
            RegisterV2Result mockResult = new RegisterV2Result("login-refresh-token", mockResponse);
            given(emailAuthService.loginWithEmail(any())).willReturn(mockResult);
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            controller.loginWithEmail(request, httpResponse);

            assertThat(httpResponse.getHeaders("Set-Cookie"))
                .anyMatch(h -> h.contains("login-refresh-token"));
        }
    }

    // ── checkEmail ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkEmail - 이메일 중복 확인")
    class CheckEmail {

        @Test
        @DisplayName("사용 가능한 이메일이면 isAvailable: true를 반환한다")
        void checkEmail_사용가능() {
            given(registrationV2Service.checkEmail("new@test.com"))
                .willReturn(EmailCheckResponse.available());

            ResponseEntity<?> result = controller.checkEmail("new@test.com");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("중복 이메일이면 isAvailable: false를 반환한다")
        void checkEmail_중복() {
            given(registrationV2Service.checkEmail("dup@test.com"))
                .willReturn(EmailCheckResponse.unavailable());

            ResponseEntity<?> result = controller.checkEmail("dup@test.com");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(registrationV2Service).checkEmail("dup@test.com");
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private RegisterV2Response mockRegisterV2Response() {
        return new RegisterV2Response(
            "access-token",
            "Bearer",
            3600L,
            new RegisterV2Response.UserInfo(
                "1", "홍길동", "홍길동", "user@test.com",
                null, null, false
            )
        );
    }
}

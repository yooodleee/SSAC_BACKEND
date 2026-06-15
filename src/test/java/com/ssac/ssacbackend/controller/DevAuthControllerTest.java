package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.service.DevUserService;
import com.ssac.ssacbackend.service.PendingRegistrationService;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("DevAuthController")
class DevAuthControllerTest {

    private PendingRegistrationService pendingRegistrationService;
    private DevUserService devUserService;
    private DevAuthController controller;

    @BeforeEach
    void setUp() {
        pendingRegistrationService = mock(PendingRegistrationService.class);
        devUserService = mock(DevUserService.class);
        controller = new DevAuthController(pendingRegistrationService, devUserService);
        ReflectionTestUtils.setField(controller, "defaultRedirectUri", "http://localhost:3000");
    }

    // ── mockNewUser ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mockNewUser - 신규 회원 가입 플로우 모의")
    class MockNewUser {

        @Test
        @DisplayName("redirect=true(기본)이면 카카오 FE 콜백 URL로 리다이렉트한다")
        void mockNewUser_KAKAO_리다이렉트() throws IOException {
            given(pendingRegistrationService.create(eq(OAuthProvider.KAKAO), anyString(), anyString()))
                .willReturn("temp-token-abc");
            MockHttpServletResponse response = new MockHttpServletResponse();

            controller.mockNewUser(OAuthProvider.KAKAO, true, response);

            assertThat(response.getRedirectedUrl()).contains("/auth/kakao/callback");
            assertThat(response.getRedirectedUrl()).contains("isNewUser=true");
            assertThat(response.getRedirectedUrl()).contains("tempToken=temp-token-abc");
        }

        @Test
        @DisplayName("redirect=true이면 네이버 FE 콜백 URL로 리다이렉트한다")
        void mockNewUser_NAVER_리다이렉트() throws IOException {
            given(pendingRegistrationService.create(eq(OAuthProvider.NAVER), anyString(), anyString()))
                .willReturn("naver-temp-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            controller.mockNewUser(OAuthProvider.NAVER, true, response);

            assertThat(response.getRedirectedUrl()).contains("/auth/naver/callback");
            assertThat(response.getRedirectedUrl()).contains("isNewUser=true");
        }

        @Test
        @DisplayName("redirect=false이면 JSON으로 tempToken을 반환한다")
        void mockNewUser_JSON응답() throws IOException {
            given(pendingRegistrationService.create(eq(OAuthProvider.KAKAO), anyString(), anyString()))
                .willReturn("json-temp-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            Object result = controller.mockNewUser(OAuthProvider.KAKAO, false, response);

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(DevAuthController.MockNewUserResponse.class);
            DevAuthController.MockNewUserResponse mockResponse =
                (DevAuthController.MockNewUserResponse) result;
            assertThat(mockResponse.isNewUser()).isTrue();
            assertThat(mockResponse.tempToken()).isEqualTo("json-temp-token");
        }
    }

    // ── deleteUser ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteUser - 테스트 사용자 삭제")
    class DeleteUser {

        @Test
        @DisplayName("이메일로 테스트 사용자 삭제 성공 시 200을 반환한다")
        void deleteUser_성공() {
            ResponseEntity<Map<String, String>> result =
                controller.deleteUser("test@example.com");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsEntry("email", "test@example.com");
            verify(devUserService).deleteByEmail("test@example.com");
        }

        @Test
        @DisplayName("삭제 성공 응답에 message 필드를 포함한다")
        void deleteUser_응답에message포함() {
            ResponseEntity<Map<String, String>> result =
                controller.deleteUser("another@example.com");

            assertThat(result.getBody()).containsKey("message");
        }
    }
}

package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.request.UpdateNicknameRequest;
import com.ssac.ssacbackend.dto.response.MyPageResponse;
import com.ssac.ssacbackend.service.ProfileService;
import com.ssac.ssacbackend.service.TokenService;
import com.ssac.ssacbackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

@DisplayName("ProfileController")
class ProfileControllerTest {

    private ProfileService profileService;
    private UserService userService;
    private TokenService tokenService;
    private ProfileController controller;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        userService = mock(UserService.class);
        tokenService = mock(TokenService.class);
        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");
        controller = new ProfileController(profileService, userService, tokenService, cookieProperties);
    }

    // ── getProfile ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProfile - 내 프로필 조회")
    class GetProfile {

        @Test
        @DisplayName("프로필 조회 성공 시 200과 MyPageResponse를 반환한다")
        void getProfile_성공() {
            Authentication auth = mockAuth("user@test.com");
            MyPageResponse mockProfile = mock(MyPageResponse.class);
            given(userService.getMyPage("user@test.com")).willReturn(mockProfile);

            ResponseEntity<ApiResponse<MyPageResponse>> result = controller.getProfile(auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(mockProfile);
            verify(userService).getMyPage("user@test.com");
        }
    }

    // ── updateNickname ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateNickname - 닉네임 수정")
    class UpdateNickname {

        @Test
        @DisplayName("닉네임 수정 성공 시 204를 반환한다")
        void updateNickname_성공() {
            Authentication auth = mockAuth("user@test.com");
            UpdateNicknameRequest request = new UpdateNicknameRequest("새닉네임");

            ResponseEntity<Void> result = controller.updateNickname(auth, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(profileService).updateNickname("user@test.com", "새닉네임");
        }
    }

    // ── logoutAll ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logoutAll - 전체 디바이스 로그아웃")
    class LogoutAll {

        @Test
        @DisplayName("전체 로그아웃 성공 시 204를 반환한다")
        void logoutAll_성공() {
            Authentication auth = mockAuth("user@test.com");
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            ResponseEntity<Void> result = controller.logoutAll(auth, httpResponse);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(tokenService).logoutAll("user@test.com");
        }

        @Test
        @DisplayName("전체 로그아웃 성공 시 쿠키 삭제 헤더가 포함된다")
        void logoutAll_쿠키삭제() {
            Authentication auth = mockAuth("user@test.com");
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            controller.logoutAll(auth, httpResponse);

            assertThat(httpResponse.getHeaders("Set-Cookie"))
                .anyMatch(h -> h.contains("Max-Age=0"));
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }
}

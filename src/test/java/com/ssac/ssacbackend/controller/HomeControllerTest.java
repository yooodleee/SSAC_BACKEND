package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.service.HomeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@DisplayName("HomeController")
class HomeControllerTest {

    private HomeService homeService;
    private HomeController controller;

    @BeforeEach
    void setUp() {
        homeService = mock(HomeService.class);
        controller = new HomeController(homeService);
    }

    // ── getHome ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHome - 홈 화면 조회")
    class GetHome {

        @Test
        @DisplayName("홈 화면 조회 성공 시 200과 결과를 반환한다")
        void getHome_성공() {
            Authentication auth = mockAuth("user@test.com");
            Object mockResult = new Object();
            given(homeService.getHome("user@test.com")).willReturn(mockResult);

            ResponseEntity<ApiResponse<Object>> result = controller.getHome(auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(mockResult);
        }

        @Test
        @DisplayName("홈 화면 조회 시 인증 사용자 이메일을 서비스에 전달한다")
        void getHome_이메일전달() {
            Authentication auth = mockAuth("user@test.com");
            given(homeService.getHome("user@test.com")).willReturn(new Object());

            controller.getHome(auth);

            verify(homeService).getHome("user@test.com");
        }

        @Test
        @DisplayName("온보딩 미완료 사용자의 경우 서비스가 onboardingRequired 응답을 반환한다")
        void getHome_온보딩미완료() {
            Authentication auth = mockAuth("new@test.com");
            String onboardingResult = "{\"onboardingRequired\": true}";
            given(homeService.getHome("new@test.com")).willReturn(onboardingResult);

            ResponseEntity<ApiResponse<Object>> result = controller.getHome(auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData()).isEqualTo(onboardingResult);
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }
}

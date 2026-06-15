package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.RecommendationResponse;
import com.ssac.ssacbackend.service.RecommendationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@DisplayName("RecommendationController")
class RecommendationControllerTest {

    private RecommendationService recommendationService;
    private RecommendationController controller;

    @BeforeEach
    void setUp() {
        recommendationService = mock(RecommendationService.class);
        controller = new RecommendationController(recommendationService);
    }

    // ── getRecommendations ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRecommendations - 개인화 퀴즈 추천 조회")
    class GetRecommendations {

        @Test
        @DisplayName("기존 사용자 추천 조회 성공 시 200과 personalized:true 응답을 반환한다")
        void getRecommendations_기존사용자() {
            Authentication auth = mockAuth("user@test.com");
            RecommendationResponse mockResponse = new RecommendationResponse(true, List.of());
            given(recommendationService.getRecommendations("user@test.com"))
                .willReturn(mockResponse);

            ResponseEntity<ApiResponse<RecommendationResponse>> result =
                controller.getRecommendations(auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData().personalized()).isTrue();
        }

        @Test
        @DisplayName("신규 사용자 추천 조회 시 personalized:false 응답을 반환한다")
        void getRecommendations_신규사용자() {
            Authentication auth = mockAuth("new@test.com");
            RecommendationResponse mockResponse = new RecommendationResponse(false, List.of());
            given(recommendationService.getRecommendations("new@test.com"))
                .willReturn(mockResponse);

            ResponseEntity<ApiResponse<RecommendationResponse>> result =
                controller.getRecommendations(auth);

            assertThat(result.getBody().getData().personalized()).isFalse();
        }

        @Test
        @DisplayName("추천 조회 시 인증 사용자 이메일을 서비스에 전달한다")
        void getRecommendations_이메일전달() {
            Authentication auth = mockAuth("user@test.com");
            given(recommendationService.getRecommendations("user@test.com"))
                .willReturn(new RecommendationResponse(true, List.of()));

            controller.getRecommendations(auth);

            verify(recommendationService).getRecommendations("user@test.com");
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }
}

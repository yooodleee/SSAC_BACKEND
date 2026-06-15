package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.request.UpdateProgressRequest;
import com.ssac.ssacbackend.dto.response.ResumeContentResponse;
import com.ssac.ssacbackend.dto.response.ResumeResponse;
import com.ssac.ssacbackend.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@DisplayName("ResumeController")
class ResumeControllerTest {

    private ResumeService resumeService;
    private ResumeController controller;

    @BeforeEach
    void setUp() {
        resumeService = mock(ResumeService.class);
        controller = new ResumeController(resumeService);
    }

    // ── getResume ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getResume - 이어보기 조회")
    class GetResume {

        @Test
        @DisplayName("이어보기 콘텐츠가 있는 경우 200과 hasResume: true를 반환한다")
        void getResume_콘텐츠있음() {
            Authentication auth = mockAuth("user@test.com");
            ResumeContentResponse content = new ResumeContentResponse("1", "스프링 기초", "00:05:30", 40);
            ResumeResponse mockResponse = ResumeResponse.of(content);
            given(resumeService.getResume("user@test.com")).willReturn(mockResponse);

            ResponseEntity<ApiResponse<ResumeResponse>> result = controller.getResume(auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData().hasResume()).isTrue();
        }

        @Test
        @DisplayName("이어보기 콘텐츠가 없는 경우 200과 hasResume: false를 반환한다")
        void getResume_콘텐츠없음() {
            Authentication auth = mockAuth("user@test.com");
            given(resumeService.getResume("user@test.com")).willReturn(ResumeResponse.empty());

            ResponseEntity<ApiResponse<ResumeResponse>> result = controller.getResume(auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData().hasResume()).isFalse();
        }
    }

    // ── updateProgress ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProgress - 진행 상황 갱신")
    class UpdateProgress {

        @Test
        @DisplayName("진행 상황 갱신 성공 시 200과 ResumeContentResponse를 반환한다")
        void updateProgress_성공() {
            Authentication auth = mockAuth("user@test.com");
            UpdateProgressRequest request = new UpdateProgressRequest("00:05:30", 40);
            ResumeContentResponse mockResult = new ResumeContentResponse("1", "스프링 기초", "00:05:30", 40);
            given(resumeService.updateProgress(1L, "user@test.com", "00:05:30", 40))
                .willReturn(mockResult);

            ResponseEntity<ApiResponse<ResumeContentResponse>> result =
                controller.updateProgress(1L, request, auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData()).isEqualTo(mockResult);
        }

        @Test
        @DisplayName("진행 상황 갱신 시 올바른 인자를 서비스에 전달한다")
        void updateProgress_인자전달() {
            Authentication auth = mockAuth("user@test.com");
            UpdateProgressRequest request = new UpdateProgressRequest("00:10:00", 75);
            ResumeContentResponse mockResult = mock(ResumeContentResponse.class);
            given(resumeService.updateProgress(99L, "user@test.com", "00:10:00", 75))
                .willReturn(mockResult);

            controller.updateProgress(99L, request, auth);

            verify(resumeService).updateProgress(99L, "user@test.com", "00:10:00", 75);
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }
}

package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.request.FeedbackRequest;
import com.ssac.ssacbackend.dto.response.FeedbackResponse;
import com.ssac.ssacbackend.service.FeedbackService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("FeedbackController")
class FeedbackControllerTest {

    private FeedbackService feedbackService;
    private FeedbackController controller;

    @BeforeEach
    void setUp() {
        feedbackService = mock(FeedbackService.class);
        controller = new FeedbackController(feedbackService);
    }

    // ── submitFeedback ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitFeedback - 피드백 전송")
    class SubmitFeedback {

        @Test
        @DisplayName("피드백 전송 성공 시 201과 FeedbackResponse를 반환한다")
        void submitFeedback_성공() {
            FeedbackRequest request = new FeedbackRequest("버그가 있어요", "user-1", "/quiz");
            FeedbackResponse mockResponse = new FeedbackResponse("fb-001", LocalDateTime.now());
            given(feedbackService.submit(request)).willReturn(mockResponse);

            ResponseEntity<ApiResponse<FeedbackResponse>> result =
                controller.submitFeedback(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(mockResponse);
        }

        @Test
        @DisplayName("비로그인 사용자도 피드백을 전송할 수 있다 (userId null 허용)")
        void submitFeedback_비로그인() {
            FeedbackRequest request = new FeedbackRequest("의견이에요", null, "/home");
            FeedbackResponse mockResponse = new FeedbackResponse("fb-002", LocalDateTime.now());
            given(feedbackService.submit(request)).willReturn(mockResponse);

            ResponseEntity<ApiResponse<FeedbackResponse>> result =
                controller.submitFeedback(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(feedbackService).submit(request);
        }
    }
}

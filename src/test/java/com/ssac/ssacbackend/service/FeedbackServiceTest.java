package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.domain.feedback.Feedback;
import com.ssac.ssacbackend.dto.request.FeedbackRequest;
import com.ssac.ssacbackend.dto.response.FeedbackResponse;
import com.ssac.ssacbackend.repository.FeedbackRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    @DisplayName("로그인 사용자 피드백 전송 성공")
    void 로그인_사용자_피드백_전송_성공() {
        FeedbackRequest request = new FeedbackRequest("좋은 서비스입니다.", "42", "https://example.com/home");
        Feedback saved = buildFeedback(1L, 42L, "좋은 서비스입니다.", "https://example.com/home");
        given(feedbackRepository.save(any(Feedback.class))).willReturn(saved);

        FeedbackResponse response = feedbackService.submit(request);

        assertThat(response.feedbackId()).isEqualTo("1");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("비로그인 사용자 피드백 전송 성공 (userId null)")
    void 비로그인_사용자_피드백_전송_성공() {
        FeedbackRequest request = new FeedbackRequest("버그가 있어요.", null, "https://example.com/quiz");
        Feedback saved = buildFeedback(2L, null, "버그가 있어요.", "https://example.com/quiz");
        given(feedbackRepository.save(any(Feedback.class))).willReturn(saved);

        FeedbackResponse response = feedbackService.submit(request);

        assertThat(response.feedbackId()).isEqualTo("2");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("메시지 미입력 시 400 / FEEDBACK-001")
    void 메시지_미입력_시_400_FEEDBACK_001() {
        FeedbackRequest request = new FeedbackRequest("", null, "https://example.com");

        assertThatThrownBy(() -> feedbackService.submit(request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> {
                BadRequestException bre = (BadRequestException) ex;
                assertThat(bre.getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_MESSAGE_REQUIRED);
                assertThat(bre.getErrorCode().getCode()).isEqualTo("FEEDBACK-001");
            });
    }

    @Test
    @DisplayName("메시지 1000자 초과 시 400 / FEEDBACK-002")
    void 메시지_1000자_초과_시_400_FEEDBACK_002() {
        String longMessage = "a".repeat(1001);
        FeedbackRequest request = new FeedbackRequest(longMessage, null, "https://example.com");

        assertThatThrownBy(() -> feedbackService.submit(request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> {
                BadRequestException bre = (BadRequestException) ex;
                assertThat(bre.getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_MESSAGE_TOO_LONG);
                assertThat(bre.getErrorCode().getCode()).isEqualTo("FEEDBACK-002");
            });
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Feedback buildFeedback(Long id, Long userId, String message, String pageUrl) {
        Feedback feedback = Feedback.builder()
            .userId(userId)
            .message(message)
            .pageUrl(pageUrl)
            .build();
        ReflectionTestUtils.setField(feedback, "id", id);
        ReflectionTestUtils.setField(feedback, "createdAt", LocalDateTime.now());
        return feedback;
    }
}

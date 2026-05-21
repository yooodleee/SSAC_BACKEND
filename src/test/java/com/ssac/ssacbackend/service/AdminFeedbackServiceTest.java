package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.domain.feedback.Feedback;
import com.ssac.ssacbackend.domain.feedback.FeedbackStatus;
import com.ssac.ssacbackend.dto.response.FeedbackListResponse;
import com.ssac.ssacbackend.repository.FeedbackRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminFeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminFeedbackService adminFeedbackService;

    // ── 닉네임 마스킹 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("닉네임 마스킹 - 세 글자: 마지막 한 글자 *")
    void 닉네임_마스킹_세글자() {
        assertThat(AdminFeedbackService.maskNickname("홍길동")).isEqualTo("홍길*");
    }

    @Test
    @DisplayName("닉네임 마스킹 - 두 글자: 마지막 한 글자 *")
    void 닉네임_마스킹_두글자() {
        assertThat(AdminFeedbackService.maskNickname("ab")).isEqualTo("a*");
    }

    @Test
    @DisplayName("닉네임 마스킹 - 한 글자: 전체 *")
    void 닉네임_마스킹_한글자() {
        assertThat(AdminFeedbackService.maskNickname("a")).isEqualTo("*");
    }

    @Test
    @DisplayName("닉네임 마스킹 - null: *")
    void 닉네임_마스킹_null() {
        assertThat(AdminFeedbackService.maskNickname(null)).isEqualTo("*");
    }

    // ── 피드백 상태 필터링 ────────────────────────────────────────────────────

    @Test
    @DisplayName("피드백 상태 필터링 - PENDING 상태만 조회")
    void 피드백_상태_필터링_PENDING() {
        Feedback f = buildFeedback(1L, null, FeedbackStatus.PENDING);
        given(feedbackRepository.findByStatusOrderByCreatedAtDesc(FeedbackStatus.PENDING, PageRequest.of(0, 20)))
            .willReturn(new PageImpl<>(List.of(f)));
        given(feedbackRepository.countByStatus(FeedbackStatus.PENDING)).willReturn(1L);

        FeedbackListResponse response = adminFeedbackService.getFeedbacks(FeedbackStatus.PENDING, 1, 20);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.feedbacks()).hasSize(1);
        assertThat(response.feedbacks().get(0).status()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("피드백 목록 - 비로그인 사용자는 maskedNickname이 *")
    void 비로그인_피드백_닉네임_마스킹() {
        Feedback f = buildFeedback(1L, null, FeedbackStatus.PENDING);
        given(feedbackRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
            .willReturn(new PageImpl<>(List.of(f)));
        given(feedbackRepository.count()).willReturn(1L);

        FeedbackListResponse response = adminFeedbackService.getFeedbacks(null, 1, 20);

        assertThat(response.feedbacks().get(0).maskedNickname()).isEqualTo("*");
    }

    // ── 피드백 상태 변경 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("피드백 상태 변경 성공")
    void 피드백_상태_변경_성공() {
        Feedback f = buildFeedback(1L, null, FeedbackStatus.PENDING);
        given(feedbackRepository.findById(1L)).willReturn(Optional.of(f));

        adminFeedbackService.updateStatus(1L, FeedbackStatus.DONE);

        assertThat(f.getStatus()).isEqualTo(FeedbackStatus.DONE);
    }

    @Test
    @DisplayName("존재하지 않는 피드백 상태 변경 시 404 / FEEDBACK-003")
    void 존재하지_않는_피드백_상태_변경_시_404() {
        given(feedbackRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminFeedbackService.updateStatus(999L, FeedbackStatus.DONE))
            .isInstanceOf(NotFoundException.class)
            .satisfies(ex -> assertThat(((NotFoundException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FEEDBACK_NOT_FOUND));
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Feedback buildFeedback(Long id, Long userId, FeedbackStatus status) {
        Feedback f = Feedback.builder()
            .userId(userId)
            .message("테스트 피드백")
            .pageUrl("https://example.com")
            .build();
        ReflectionTestUtils.setField(f, "id", id);
        ReflectionTestUtils.setField(f, "status", status);
        ReflectionTestUtils.setField(f, "createdAt", LocalDateTime.now());
        return f;
    }
}

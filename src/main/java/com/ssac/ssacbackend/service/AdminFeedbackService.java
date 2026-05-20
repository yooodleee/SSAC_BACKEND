package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.feedback.Feedback;
import com.ssac.ssacbackend.domain.feedback.FeedbackStatus;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.response.FeedbackListResponse;
import com.ssac.ssacbackend.dto.response.FeedbackListResponse.FeedbackItem;
import com.ssac.ssacbackend.repository.FeedbackRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 전용 피드백 관리 서비스.
 */
@Service
@RequiredArgsConstructor
public class AdminFeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    /**
     * 피드백 목록을 최신순으로 조회한다. status 필터링을 지원한다.
     *
     * @param status null이면 전체 조회
     * @param page   1-based 페이지 번호
     * @param size   페이지 크기
     */
    @Transactional(readOnly = true)
    public FeedbackListResponse getFeedbacks(FeedbackStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<Feedback> feedbackPage;
        long totalCount;

        if (status != null) {
            feedbackPage = feedbackRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
            totalCount = feedbackRepository.countByStatus(status);
        } else {
            feedbackPage = feedbackRepository.findAllByOrderByCreatedAtDesc(pageable);
            totalCount = feedbackRepository.count();
        }

        List<FeedbackItem> items = feedbackPage.getContent().stream()
            .map(f -> new FeedbackItem(
                String.valueOf(f.getId()),
                f.getStatus().name(),
                f.getMessage(),
                resolveNickname(f.getUserId()),
                f.getPageUrl(),
                f.getCreatedAt()
            ))
            .toList();

        return new FeedbackListResponse(totalCount, items);
    }

    /**
     * 피드백 상태를 변경한다.
     */
    @Transactional
    public void updateStatus(Long feedbackId, FeedbackStatus newStatus) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.FEEDBACK_NOT_FOUND));
        feedback.updateStatus(newStatus);
    }

    /**
     * userId로 닉네임을 조회하여 마스킹한다.
     */
    private String resolveNickname(Long userId) {
        if (userId == null) {
            return "*";
        }
        return userRepository.findById(userId)
            .map(u -> maskNickname(u.getDisplayNickname()))
            .orElse("*");
    }

    /**
     * 닉네임 마스킹: 마지막 글자를 * 처리한다.
     *
     * <p>예) "홍길동" → "홍길*" / "ab" → "a*" / "a" → "*"
     */
    static String maskNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return "*";
        }
        int visibleLength = Math.max(0, nickname.length() - 1);
        return nickname.substring(0, visibleLength) + "*".repeat(nickname.length() - visibleLength);
    }

    /**
     * 관리자 홈용 통계: 전체 사용자 수 (GUEST 제외).
     */
    public long countNonGuestUsers() {
        return userRepository.countByRoleNot(UserRole.GUEST);
    }

    /**
     * 관리자 홈용 통계: 전체 피드백 수.
     */
    public long countTotalFeedbacks() {
        return feedbackRepository.count();
    }

    /**
     * 관리자 홈용 통계: PENDING 피드백 수.
     */
    public long countPendingFeedbacks() {
        return feedbackRepository.countByStatus(FeedbackStatus.PENDING);
    }
}

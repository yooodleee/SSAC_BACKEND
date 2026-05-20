package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.domain.feedback.Feedback;
import com.ssac.ssacbackend.dto.request.FeedbackRequest;
import com.ssac.ssacbackend.dto.response.FeedbackResponse;
import com.ssac.ssacbackend.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드백 전송 서비스.
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public FeedbackResponse submit(FeedbackRequest request) {
        validateMessage(request.message());

        Long userId = parseUserId(request.userId());

        Feedback feedback = Feedback.builder()
            .userId(userId)
            .message(request.message())
            .pageUrl(request.pageUrl())
            .build();

        Feedback saved = feedbackRepository.save(feedback);
        return new FeedbackResponse(
            String.valueOf(saved.getId()),
            saved.getCreatedAt()
        );
    }

    private void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new BadRequestException(ErrorCode.FEEDBACK_MESSAGE_REQUIRED);
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new BadRequestException(ErrorCode.FEEDBACK_MESSAGE_TOO_LONG);
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

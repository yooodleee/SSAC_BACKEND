package com.ssac.ssacbackend.dto.request;

import com.ssac.ssacbackend.domain.feedback.FeedbackStatus;

/**
 * 피드백 상태 변경 요청 DTO.
 */
public record FeedbackStatusUpdateRequest(
    FeedbackStatus status
) {
}

package com.ssac.ssacbackend.dto.response;

import java.time.LocalDateTime;

/**
 * 피드백 전송 성공 응답 DTO.
 */
public record FeedbackResponse(
    String feedbackId,
    LocalDateTime createdAt
) {
}

package com.ssac.ssacbackend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 피드백 목록 조회 응답 DTO.
 */
public record FeedbackListResponse(
    long totalCount,
    List<FeedbackItem> feedbacks
) {
    public record FeedbackItem(
        String id,
        String status,
        String message,
        String maskedNickname,
        String pageUrl,
        LocalDateTime createdAt
    ) {
    }
}

package com.ssac.ssacbackend.dto.request;

/**
 * 피드백 전송 요청 DTO.
 *
 * <p>userId는 비로그인 시 null이다.
 */
public record FeedbackRequest(
    String message,
    String userId,
    String pageUrl
) {
}

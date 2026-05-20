package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.request.FeedbackRequest;
import com.ssac.ssacbackend.dto.response.FeedbackResponse;
import com.ssac.ssacbackend.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 피드백 전송 API.
 *
 * <p>인증 불필요 — 비로그인 사용자도 피드백을 전송할 수 있다.
 */
@Tag(name = "Feedback", description = "개발팀 문의 피드백 API")
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(
        summary = "피드백 전송",
        description = "사용자가 개발팀에 피드백을 전송한다. 비로그인 사용자는 userId를 null로 보낸다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedback(
        @RequestBody FeedbackRequest request
    ) {
        FeedbackResponse response = feedbackService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }
}

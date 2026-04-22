package com.ssac.ssacbackend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 기간별 퀴즈 통계 응답 DTO.
 *
 * <p>일간/주간/월간 단위로 집계된 통계를 담는다.
 */
public record PeriodStatResponse(

    @Schema(description = "기간 레이블 (DAILY: yyyy-MM-dd, WEEKLY: yyyy-Www, MONTHLY: yyyy-MM)",
        example = "2026-04-22")
    String label,

    @Schema(description = "해당 기간 응시 횟수", example = "3")
    int attemptCount,

    @Schema(description = "해당 기간 총 점수", example = "240")
    int totalScore,

    @Schema(description = "해당 기간 평균 점수", example = "80.0")
    double averageScore,

    @Schema(description = "해당 기간 정답률 (%)", example = "75.0")
    double accuracyRate

) {
}

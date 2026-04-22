package com.ssac.ssacbackend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 사용자 퀴즈 통계 응답 DTO.
 *
 * <p>누적 통계와 기간별 통계를 함께 반환한다.
 */
public record UserStatsResponse(

    @Schema(description = "누적 총 점수", example = "450")
    long totalScore,

    @Schema(description = "총 응시 횟수", example = "6")
    long totalAttempts,

    @Schema(description = "평균 점수", example = "75.0")
    double averageScore,

    @Schema(description = "누적 정답 문항 수", example = "42")
    long totalCorrect,

    @Schema(description = "누적 응시 문항 수", example = "60")
    long totalQuestions,

    @Schema(description = "누적 정답률 (%)", example = "70.0")
    double accuracyRate,

    @Schema(description = "기간별 통계 (최신 순)")
    List<PeriodStatResponse> periodStats

) {

    /**
     * 응시 기록이 없는 경우의 빈 통계 응답.
     */
    public static UserStatsResponse empty() {
        return new UserStatsResponse(0, 0, 0.0, 0, 0, 0.0, List.of());
    }
}

package com.ssac.ssacbackend.dto.response;

/**
 * 레벨업 진행률 DTO.
 *
 * <p>레벨업 조건 미충족 시 현재 진행 상황을 담아 반환한다.
 */
public record LevelUpProgressDto(
    int contentsProgress,
    int quizCorrectRate,
    int contentsRequired,
    int quizRequired
) {
}

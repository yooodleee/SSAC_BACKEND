package com.ssac.ssacbackend.dto.response;

/**
 * 콘텐츠 완료 처리 응답 DTO.
 *
 * <p>레벨업이 발생한 경우 previousLevel, newLevel을 포함한다.
 */
public record ContentCompleteResponse(
    String contentId,
    boolean isCompleted,
    boolean isLevelUp,
    String previousLevel,
    String newLevel
) {
}

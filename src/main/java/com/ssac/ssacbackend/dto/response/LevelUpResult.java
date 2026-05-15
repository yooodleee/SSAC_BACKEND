package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.user.UserLevel;

/**
 * 레벨업 판정 결과 내부 값 객체.
 *
 * <p>LevelUpService가 반환하며 각 API 응답 DTO 생성에 사용된다.
 */
public record LevelUpResult(
    boolean leveledUp,
    UserLevel previousLevel,
    UserLevel newLevel,
    UserLevel currentLevel,
    LevelUpProgressDto progress
) {

    /**
     * 레벨업이 발생한 경우의 결과를 생성한다.
     */
    public static LevelUpResult levelUp(UserLevel previous, UserLevel next) {
        return new LevelUpResult(true, previous, next, next, null);
    }

    /**
     * 레벨업이 발생하지 않은 경우의 결과를 생성한다.
     */
    public static LevelUpResult noLevelUp(UserLevel current, LevelUpProgressDto progress) {
        return new LevelUpResult(false, null, null, current, progress);
    }
}

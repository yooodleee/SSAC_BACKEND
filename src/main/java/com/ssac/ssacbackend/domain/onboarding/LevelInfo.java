package com.ssac.ssacbackend.domain.onboarding;

import com.ssac.ssacbackend.domain.user.UserLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 레벨별 표시 정보 (라벨, 이모지, 설명).
 */
@Getter
@RequiredArgsConstructor
public enum LevelInfo {

    SEED(UserLevel.SEED, "씨앗", "🌱", "금융이 완전 처음이에요"),
    SPROUT(UserLevel.SPROUT, "새싹", "🌿", "조금은 알고 있어요"),
    TREE(UserLevel.TREE, "나무", "🌳", "어느 정도 알고 있어요");

    private final UserLevel level;
    private final String label;
    private final String emoji;
    private final String description;

    public static LevelInfo from(UserLevel level) {
        for (LevelInfo info : values()) {
            if (info.level == level) {
                return info;
            }
        }
        throw new IllegalArgumentException("Unknown level: " + level);
    }
}

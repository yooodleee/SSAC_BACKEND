package com.ssac.ssacbackend.domain.user;

/**
 * 사용자 레벨.
 *
 * <p>온보딩 퀴즈 결과에 따라 결정된다. null이면 퀴즈를 아직 완료하지 않은 상태다.
 */
public enum UserLevel {
    SEED,    // 씨앗 (기본값 / 건너뛰기 선택 시)
    SPROUT,  // 새싹
    TREE     // 나무
}

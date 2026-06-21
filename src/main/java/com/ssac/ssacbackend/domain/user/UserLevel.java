package com.ssac.ssacbackend.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 레벨.
 *
 * <p>온보딩 퀴즈 결과에 따라 결정된다. null이면 퀴즈를 아직 완료하지 않은 상태다.
 *
 * <p>콘텐츠 난이도와 동일한 계층 구조를 공유하므로 콘텐츠 도메인에서도 이 enum을 직접 사용한다.
 * {@link #contentLabel}은 콘텐츠 카드 및 Notion 동기화 시 표시되는 난이도 레이블이다.
 */
@Getter
@RequiredArgsConstructor
public enum UserLevel {

    SEED("씨앗", "🌱", "금융이 완전 처음이에요", "왕초보"),       // 기본값 / 건너뛰기 선택 시
    SPROUT("새싹", "🌿", "조금은 알고 있어요", "초보"),
    TREE("나무", "🌳", "어느 정도 알고 있어요", "중급");

    /** 사용자 프로필·온보딩 결과 화면에 표시되는 레벨명. */
    private final String label;

    /** 레벨 아이콘 이모지. */
    private final String emoji;

    /** 온보딩 결과 화면에 표시되는 레벨 설명. */
    private final String description;

    /**
     * 콘텐츠 카드 및 Notion 동기화 시 표시되는 난이도 레이블.
     * 이전에는 {@code ContentDifficulty}, {@code NotionSyncService}에 분산되어 있었다.
     */
    private final String contentLabel;
}

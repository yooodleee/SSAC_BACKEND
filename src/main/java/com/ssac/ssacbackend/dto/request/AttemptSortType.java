package com.ssac.ssacbackend.dto.request;

/**
 * 퀴즈 응시 기록 정렬 기준.
 *
 * <p>Controller → Service 레이어에서 정렬 옵션을 전달하는 데 사용한다.
 * ArchUnit 규칙(Controller는 Domain에 접근 불가)에 따라 dto 패키지에 위치한다.
 */
public enum AttemptSortType {

    /** 최신 응시 순 (attemptedAt DESC). */
    LATEST,

    /** 높은 점수 순 (earnedScore DESC, attemptedAt DESC). */
    SCORE
}

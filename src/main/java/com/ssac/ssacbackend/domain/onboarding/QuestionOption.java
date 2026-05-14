package com.ssac.ssacbackend.domain.onboarding;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 온보딩 테스트 고정 선택지.
 *
 * <p>모든 문제에 동일하게 적용되는 3개 선택지와 점수 정의.
 */
@Getter
@RequiredArgsConstructor
public enum QuestionOption {
    A("네, 잘 알아요", 2),
    B("들어봤는데 잘 몰라요", 1),
    C("처음 들어봐요", 0);

    private final String label;
    private final int score;
}

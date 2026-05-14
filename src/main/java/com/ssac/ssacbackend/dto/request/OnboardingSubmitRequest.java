package com.ssac.ssacbackend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * 온보딩 테스트 응답 제출 요청 DTO.
 *
 * <p>answers 크기(5개) 검증은 서비스 레이어에서 ONBOARDING-003 코드로 처리한다.
 */
public record OnboardingSubmitRequest(
    @NotNull List<@Valid Answer> answers
) {

    public record Answer(
        @NotNull Long questionId,
        @NotNull @Pattern(regexp = "^[ABC]$", message = "선택지는 A, B, C 중 하나여야 합니다.")
        String selectedOption
    ) {}
}

package com.ssac.ssacbackend.dto.response;

import com.ssac.ssacbackend.domain.onboarding.OnboardingQuestion;
import com.ssac.ssacbackend.domain.user.UserType;
import java.util.List;

/**
 * 온보딩 테스트 문제 목록 응답 DTO.
 *
 * <p>선택지(options)는 모든 문제에 고정 적용된다.
 */
public record OnboardingQuestionsResponse(
    UserType userType,
    int totalCount,
    List<QuestionDto> questions
) {

    private static final List<OptionDto> FIXED_OPTIONS = List.of(
        new OptionDto("A", "네, 잘 알아요", 2),
        new OptionDto("B", "들어봤는데 잘 몰라요", 1),
        new OptionDto("C", "처음 들어봐요", 0)
    );

    public static OnboardingQuestionsResponse from(UserType userType,
        List<OnboardingQuestion> questions) {
        List<QuestionDto> dtos = questions.stream()
            .map(q -> new QuestionDto(q.getId(), q.getQuestionOrder(), q.getContent(), FIXED_OPTIONS))
            .toList();
        return new OnboardingQuestionsResponse(userType, dtos.size(), dtos);
    }

    public record QuestionDto(
        Long id,
        int order,
        String content,
        List<OptionDto> options
    ) {}

    public record OptionDto(
        String id,
        String label,
        int score
    ) {}
}

package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ConflictException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.onboarding.OnboardingQuestion;
import com.ssac.ssacbackend.domain.onboarding.QuestionOption;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.request.OnboardingSubmitRequest;
import com.ssac.ssacbackend.dto.response.OnboardingQuestionsResponse;
import com.ssac.ssacbackend.dto.response.OnboardingSkipResponse;
import com.ssac.ssacbackend.dto.response.OnboardingSubmitResponse;
import com.ssac.ssacbackend.repository.OnboardingQuestionRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 온보딩 테스트 비즈니스 로직.
 *
 * <p>문제 조회 → 응답 제출 → 레벨 자동 판정 → 저장 흐름을 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final OnboardingQuestionRepository onboardingQuestionRepository;

    /**
     * 로그인한 사용자의 userType에 맞는 온보딩 문제 목록을 반환한다.
     *
     * @param email JWT에서 추출한 사용자 이메일
     */
    @Transactional(readOnly = true)
    public OnboardingQuestionsResponse getQuestions(String email) {
        User user = findUserByEmail(email);

        if (user.getUserType() == null) {
            throw new BadRequestException(ErrorCode.ONBOARDING_USER_TYPE_MISSING);
        }

        if (user.isOnboardingCompleted()) {
            throw new ConflictException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }

        List<OnboardingQuestion> questions = onboardingQuestionRepository
            .findByUserTypeAndIsActiveTrueOrderByQuestionOrderAsc(user.getUserType());

        return OnboardingQuestionsResponse.from(user.getUserType(), questions);
    }

    /**
     * 온보딩 응답을 검증하고 레벨을 판정하여 저장한다.
     *
     * <p>레벨 판정 기준: 0~3점 SEED / 4~7점 SPROUT / 8~10점 TREE
     *
     * @param email   JWT에서 추출한 사용자 이메일
     * @param request 5개 문제의 선택 응답
     */
    @Transactional
    public OnboardingSubmitResponse submit(String email, OnboardingSubmitRequest request) {
        User user = findUserByEmail(email);

        if (user.getUserType() == null) {
            throw new BadRequestException(ErrorCode.ONBOARDING_USER_TYPE_MISSING);
        }

        if (user.isOnboardingCompleted()) {
            throw new ConflictException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }

        List<OnboardingSubmitRequest.Answer> answers = request.answers();
        if (answers.size() != 5) {
            throw new BadRequestException(ErrorCode.ONBOARDING_INCOMPLETE_ANSWERS);
        }

        List<Long> questionIds = answers.stream()
            .map(OnboardingSubmitRequest.Answer::questionId)
            .toList();

        List<OnboardingQuestion> questions = onboardingQuestionRepository.findAllById(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new BadRequestException(ErrorCode.ONBOARDING_INVALID_QUESTION);
        }

        boolean hasTypeMismatch = questions.stream()
            .anyMatch(q -> q.getUserType() != user.getUserType());
        if (hasTypeMismatch) {
            throw new BadRequestException(ErrorCode.ONBOARDING_QUESTION_TYPE_MISMATCH);
        }

        int totalScore = answers.stream()
            .mapToInt(a -> QuestionOption.valueOf(a.selectedOption()).getScore())
            .sum();

        UserLevel level = calculateLevel(totalScore);
        user.completeOnboarding(level);
        log.debug("온보딩 완료: email={}, level={}, totalScore={}", email, level, totalScore);

        return new OnboardingSubmitResponse(level, totalScore, true);
    }

    /**
     * 온보딩을 건너뛰고 기본 레벨 SEED로 설정한다.
     *
     * @param email JWT에서 추출한 사용자 이메일
     */
    @Transactional
    public OnboardingSkipResponse skip(String email) {
        User user = findUserByEmail(email);
        user.completeOnboarding(UserLevel.SEED);
        log.debug("온보딩 건너뛰기: email={}", email);
        return new OnboardingSkipResponse(UserLevel.SEED, true, true);
    }

    private UserLevel calculateLevel(int totalScore) {
        if (totalScore >= 8) {
            return UserLevel.TREE;
        }
        if (totalScore >= 4) {
            return UserLevel.SPROUT;
        }
        return UserLevel.SEED;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}

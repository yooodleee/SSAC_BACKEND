package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ConflictException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.onboarding.LevelInfo;
import com.ssac.ssacbackend.domain.onboarding.OnboardingQuestion;
import com.ssac.ssacbackend.domain.onboarding.QuestionOption;
import com.ssac.ssacbackend.domain.onboarding.UserInterest;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.request.OnboardingInterestsRequest;
import com.ssac.ssacbackend.dto.request.OnboardingSubmitRequest;
import com.ssac.ssacbackend.dto.response.OnboardingQuestionsResponse;
import com.ssac.ssacbackend.dto.response.OnboardingResultResponse;
import com.ssac.ssacbackend.dto.response.OnboardingSkipResponse;
import com.ssac.ssacbackend.dto.response.OnboardingSubmitResponse;
import com.ssac.ssacbackend.dto.response.RecommendedDomainDto;
import com.ssac.ssacbackend.repository.OnboardingQuestionRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.List;
import java.util.Map;
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

    private static final int MAX_SCORE = 10;

    private static final Map<String, List<RecommendedDomainDto>> RECOMMENDATION_MAP = Map.of(
        recKey(UserType.HIGH_SCHOOL, UserLevel.SEED), List.of(
            new RecommendedDomainDto("scholarship", "학자금/장학금", "🎓",
                "학자금 대출과 장학금 혜택부터 알아보세요"),
            new RecommendedDomainDto("finance", "재테크/신용", "💰",
                "신용점수 관리, 지금 시작하면 나중에 달라져요"),
            new RecommendedDomainDto("realestate", "부동산/자취", "🏠",
                "자취를 처음 시작하는 분들에게 꼭 필요한 내용이에요")
        ),
        recKey(UserType.HIGH_SCHOOL, UserLevel.SPROUT), List.of(
            new RecommendedDomainDto("realestate", "부동산/자취", "🏠",
                "자취를 처음 시작하는 분들에게 꼭 필요한 내용이에요"),
            new RecommendedDomainDto("finance", "재테크/신용", "💰",
                "재테크의 기초부터 차근차근 쌓아보세요"),
            new RecommendedDomainDto("tax", "세금/연말정산", "📋",
                "아르바이트 세금 환급도 놓치지 마세요")
        ),
        recKey(UserType.HIGH_SCHOOL, UserLevel.TREE), List.of(
            new RecommendedDomainDto("tax", "세금/연말정산", "📋",
                "연말정산으로 세금을 돌려받아 보세요"),
            new RecommendedDomainDto("finance", "재테크/신용", "💰",
                "더 높은 수준의 재테크에 도전해보세요"),
            new RecommendedDomainDto("realestate", "부동산/자취", "🏠",
                "부동산 계약의 핵심을 이미 알고 있을 거예요")
        ),
        recKey(UserType.EARLY_CAREER, UserLevel.SEED), List.of(
            new RecommendedDomainDto("tax", "세금/연말정산", "📋",
                "사회초년생이라면 꼭 알아야 할 세금 이야기에요"),
            new RecommendedDomainDto("realestate", "부동산/자취", "🏠",
                "첫 자취방 구할 때 필요한 모든 것이에요"),
            new RecommendedDomainDto("finance", "재테크/신용", "💰",
                "신용점수 관리부터 시작해보세요")
        ),
        recKey(UserType.EARLY_CAREER, UserLevel.SPROUT), List.of(
            new RecommendedDomainDto("finance", "재테크/신용", "💰",
                "재테크로 자산을 조금씩 늘려보세요"),
            new RecommendedDomainDto("tax", "세금/연말정산", "📋",
                "연말정산 꼼꼼히 챙겨 세금 돌려받으세요"),
            new RecommendedDomainDto("realestate", "부동산/자취", "🏠",
                "내 집 마련을 위한 첫걸음을 시작해보세요")
        ),
        recKey(UserType.EARLY_CAREER, UserLevel.TREE), List.of(
            new RecommendedDomainDto("finance", "재테크/신용", "💰",
                "고급 재테크 전략으로 자산을 불려보세요"),
            new RecommendedDomainDto("tax", "세금/연말정산", "📋",
                "세금 최적화로 더 많은 돈을 아껴보세요"),
            new RecommendedDomainDto("realestate", "부동산/자취", "🏠",
                "부동산 투자의 다음 단계를 알아보세요")
        )
    );

    private static String recKey(UserType userType, UserLevel level) {
        return userType.name() + "_" + level.name();
    }

    private final UserRepository userRepository;
    private final OnboardingQuestionRepository onboardingQuestionRepository;
    private final UserInterestRepository userInterestRepository;

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
        user.completeOnboarding(level, totalScore);
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
        user.skipOnboarding();
        log.debug("온보딩 건너뛰기: email={}", email);
        return new OnboardingSkipResponse(UserLevel.SEED, true, true);
    }

    /**
     * 온보딩 레벨 판정 결과를 조회한다.
     *
     * @param email JWT에서 추출한 사용자 이메일
     */
    @Transactional(readOnly = true)
    public OnboardingResultResponse getResult(String email) {
        User user = findUserByEmail(email);

        if (!user.isOnboardingCompleted()) {
            throw new NotFoundException(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }

        LevelInfo levelInfo = LevelInfo.from(user.getLevel());
        List<RecommendedDomainDto> domains = user.getUserType() != null
            ? RECOMMENDATION_MAP.getOrDefault(
                recKey(user.getUserType(), user.getLevel()), List.of())
            : List.of();

        return new OnboardingResultResponse(
            user.getUserType() != null ? user.getUserType().name() : null,
            user.getLevel().name(),
            user.getOnboardingScore(),
            MAX_SCORE,
            levelInfo.getLabel(),
            levelInfo.getEmoji(),
            levelInfo.getDescription(),
            user.isOnboardingSkipped(),
            user.isOnboardingCompleted(),
            domains
        );
    }

    /**
     * 사용자가 선택한 관심 도메인을 저장한다. 기존 데이터는 덮어쓴다.
     *
     * @param email   JWT에서 추출한 사용자 이메일
     * @param request 관심 도메인 ID 목록 (1~3개)
     */
    @Transactional
    public void saveInterests(String email, OnboardingInterestsRequest request) {
        List<String> domainIds = request.domainIds();
        if (domainIds == null || domainIds.isEmpty() || domainIds.size() > 3) {
            throw new BadRequestException(ErrorCode.ONBOARDING_INTEREST_INVALID_COUNT);
        }

        User user = findUserByEmail(email);
        userInterestRepository.deleteByUserId(user.getId());

        List<UserInterest> interests = domainIds.stream()
            .map(domainId -> UserInterest.builder()
                .userId(user.getId())
                .domainId(domainId)
                .build())
            .toList();
        userInterestRepository.saveAll(interests);

        log.debug("관심 도메인 저장: email={}, domains={}", email, domainIds);
    }

    /**
     * 온보딩 결과를 초기화하여 재응시를 허용한다.
     *
     * <p>온보딩이 완료되지 않은 사용자가 요청하면 409를 반환한다.
     *
     * @param email 사용자 이메일
     */
    @Transactional
    public void resetOnboarding(String email) {
        User user = findUserByEmail(email);

        if (!user.isOnboardingCompleted()) {
            throw new ConflictException(ErrorCode.ONBOARDING_RETAKE_CONFLICT);
        }

        user.resetOnboarding();
        userInterestRepository.deleteByUserId(user.getId());
        log.info("온보딩 초기화 완료: email={}", email);
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

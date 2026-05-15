package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentCategory;
import com.ssac.ssacbackend.domain.content.ContentProgress;
import com.ssac.ssacbackend.domain.onboarding.LevelInfo;
import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.HomeResponse;
import com.ssac.ssacbackend.dto.response.HomeResponse.CategoryDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.ContinueLearningDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.HomeUserDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.RecommendedContentDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.TodayCardDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.TodayQuizDto;
import com.ssac.ssacbackend.dto.response.OnboardingRequiredResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.QuizRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 화면 비즈니스 로직.
 *
 * <p>사용자 유형과 레벨에 맞는 추천 콘텐츠, 오늘의 카드, 이어보기, 오늘의 퀴즈를 제공한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private static final int RECOMMENDED_MAX = 5;
    private static final String ONBOARDING_REDIRECT = "/onboarding/test";

    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;
    private final ContentRepository contentRepository;
    private final ContentProgressRepository contentProgressRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    /**
     * 홈 화면 데이터를 반환한다.
     *
     * <p>온보딩 미완료 시 {@link OnboardingRequiredResponse}를 반환한다.
     */
    @Transactional(readOnly = true)
    public Object getHome(String email) {
        User user = findUserByEmail(email);

        if (!user.isOnboardingCompleted()) {
            return new OnboardingRequiredResponse(true, ONBOARDING_REDIRECT);
        }

        List<String> interestDomains = user.getId() != null
            ? userInterestRepository.findDomainIdsByUserId(user.getId())
            : List.of();

        Set<Long> completedIds = new HashSet<>(
            contentProgressRepository.findCompletedContentIdsByUserEmail(email));

        UserLevel level = user.getLevel() != null ? user.getLevel() : UserLevel.SEED;

        List<RecommendedContentDto> recommended = buildRecommended(
            interestDomains, level, completedIds);

        TodayCardDto todayCard = buildTodayCard(
            user.getId(), interestDomains, level, completedIds);

        ContinueLearningDto continueLearning = buildContinueLearning(email);

        TodayQuizDto todayQuiz = buildTodayQuiz(user.getId(), level, email);

        List<CategoryDto> categories = buildCategories(email);

        LevelInfo levelInfo = LevelInfo.from(level);
        HomeUserDto userDto = new HomeUserDto(
            user.getNickname(),
            user.getUserType() != null ? user.getUserType().name() : null,
            level.name(),
            levelInfo.getLabel(),
            levelInfo.getEmoji()
        );

        return new HomeResponse(userDto, todayCard, recommended, continueLearning, todayQuiz, categories);
    }

    // ── 추천 콘텐츠 ────────────────────────────────────────────────────────────

    private List<RecommendedContentDto> buildRecommended(
        List<String> interestDomains, UserLevel level, Set<Long> completedIds) {

        Set<Long> addedIds = new LinkedHashSet<>();
        List<RecommendedContentDto> result = new ArrayList<>();

        // 1순위: 관심 도메인 + 사용자 레벨
        if (!interestDomains.isEmpty()) {
            contentRepository.findByCategoryInAndDifficultyOrderByViewCountDesc(interestDomains, level)
                .stream()
                .filter(c -> !completedIds.contains(c.getId()))
                .limit(RECOMMENDED_MAX)
                .forEach(c -> {
                    addedIds.add(c.getId());
                    result.add(toRecommendedDto(c, false));
                });
        }

        if (result.size() >= RECOMMENDED_MAX) {
            return result;
        }

        // 2순위: 사용자 레벨 일치 (관심 도메인 무관)
        contentRepository.findByDifficultyOrderByViewCountDesc(level)
            .stream()
            .filter(c -> !completedIds.contains(c.getId()) && !addedIds.contains(c.getId()))
            .limit((long) RECOMMENDED_MAX - result.size())
            .forEach(c -> {
                addedIds.add(c.getId());
                result.add(toRecommendedDto(c, false));
            });

        if (result.size() >= RECOMMENDED_MAX) {
            return result;
        }

        // 3순위: 전체 인기 콘텐츠
        contentRepository.findAllByOrderByViewCountDesc()
            .stream()
            .filter(c -> !completedIds.contains(c.getId()) && !addedIds.contains(c.getId()))
            .limit((long) RECOMMENDED_MAX - result.size())
            .forEach(c -> result.add(toRecommendedDto(c, false)));

        return result;
    }

    private RecommendedContentDto toRecommendedDto(Content content, boolean completed) {
        String emoji = ContentCategory.findById(content.getCategory())
            .map(ContentCategory::getEmoji).orElse("");
        return new RecommendedContentDto(
            String.valueOf(content.getId()),
            content.getTitle(),
            content.getCategory(),
            emoji,
            difficultyLabel(content.getDifficulty()),
            content.getEstimatedMinutes(),
            completed
        );
    }

    // ── 오늘의 카드 ────────────────────────────────────────────────────────────

    private TodayCardDto buildTodayCard(
        Long userId, List<String> interestDomains, UserLevel level, Set<Long> completedIds) {

        List<Content> candidates = new ArrayList<>();

        // 1순위: 관심 도메인 미완료 콘텐츠
        if (!interestDomains.isEmpty()) {
            contentRepository.findByCategoryInAndDifficultyOrderByViewCountDesc(
                    interestDomains, level)
                .stream()
                .filter(c -> !completedIds.contains(c.getId()))
                .forEach(candidates::add);
        }

        // 2순위: 사용자 레벨 미완료 콘텐츠
        if (candidates.isEmpty()) {
            contentRepository.findByDifficultyOrderByViewCountDesc(level)
                .stream()
                .filter(c -> !completedIds.contains(c.getId()))
                .forEach(candidates::add);
        }

        // 3순위: 전체 인기 콘텐츠
        if (candidates.isEmpty()) {
            contentRepository.findAllByOrderByViewCountDesc()
                .stream()
                .filter(c -> !completedIds.contains(c.getId()))
                .forEach(candidates::add);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // 당일 기준 결정론적 선택
        int index = deterministicIndex(userId, candidates.size());
        Content picked = candidates.get(index);

        String emoji = ContentCategory.findById(picked.getCategory())
            .map(ContentCategory::getEmoji).orElse("");

        return new TodayCardDto(
            String.valueOf(picked.getId()),
            picked.getTitle(),
            picked.getCategory(),
            emoji,
            picked.getEstimatedMinutes()
        );
    }

    // ── 이어보기 ────────────────────────────────────────────────────────────────

    private ContinueLearningDto buildContinueLearning(String email) {
        return contentProgressRepository.findContinueLearning(email, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(cp -> new ContinueLearningDto(
                String.valueOf(cp.getId()),
                cp.getTitle(),
                cp.getCategory(),
                cp.getProgressRate()
            ))
            .orElse(null);
    }

    // ── 오늘의 퀴즈 ────────────────────────────────────────────────────────────

    private TodayQuizDto buildTodayQuiz(Long userId, UserLevel level, String email) {
        List<Quiz> candidates = quizRepository.findUncompletedByDifficultyAndUserEmail(level, email);
        if (candidates.isEmpty()) {
            return null;
        }

        // 오답 퀴즈 우선 정렬
        Set<Long> incorrectIds = new HashSet<>(
            quizAttemptRepository.findIncorrectQuizIdsByUserEmail(email));

        List<Quiz> sorted = new ArrayList<>();
        candidates.stream().filter(q -> incorrectIds.contains(q.getId())).forEach(sorted::add);
        candidates.stream().filter(q -> !incorrectIds.contains(q.getId())).forEach(sorted::add);

        int index = deterministicIndex(userId, sorted.size());
        Quiz picked = sorted.get(index);

        return new TodayQuizDto(
            String.valueOf(picked.getId()),
            picked.getTitle(),
            picked.getCategory(),
            picked.getDifficulty() != null ? picked.getDifficulty().name() : null
        );
    }

    // ── 카테고리 ────────────────────────────────────────────────────────────────

    private List<CategoryDto> buildCategories(String email) {
        return ContentCategory.all().stream()
            .map(cat -> new CategoryDto(
                cat.getId(),
                cat.getName(),
                cat.getEmoji(),
                contentRepository.countByCategory(cat.getId()),
                contentProgressRepository.countCompletedByUserEmailAndCategory(email, cat.getId())
            ))
            .toList();
    }

    // ── 유틸리티 ────────────────────────────────────────────────────────────────

    /**
     * userId와 현재 날짜를 조합한 결정론적 인덱스.
     * 동일 사용자는 당일 동안 항상 같은 인덱스를 반환한다.
     */
    private int deterministicIndex(Long userId, int size) {
        long seed = (userId != null ? userId : 0L) + LocalDate.now().toEpochDay();
        return (int) Math.abs(seed % size);
    }

    private static String difficultyLabel(UserLevel level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case SEED -> "왕초보";
            case SPROUT -> "초보";
            case TREE -> "중급";
        };
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}

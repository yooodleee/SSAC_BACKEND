package com.ssac.ssacbackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentCategory;
import com.ssac.ssacbackend.domain.content.ContentDifficulty;
import com.ssac.ssacbackend.domain.onboarding.LevelInfo;
import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.HomeResponse;
import com.ssac.ssacbackend.dto.response.HomeResponse.CategoryDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.ContinueLearningDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.HomeUserDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.LastVisitDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.RecommendedContentDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.TodayCardDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.TodayQuizDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.WelcomeBackDto;
import com.ssac.ssacbackend.dto.response.OnboardingRequiredResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.QuizRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 화면 비즈니스 로직.
 *
 * <p>사용자 유형과 레벨에 맞는 추천 콘텐츠, 오늘의 카드, 이어보기, 오늘의 퀴즈를 제공한다.
 * 홈 데이터는 Redis에 사용자별로 캐싱되며, 당일 자정에 자동 만료된다.
 * 콘텐츠 완료·퀴즈 완료·레벨 변경·관심 도메인 변경 시 캐시가 즉시 무효화된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private static final int RECOMMENDED_MAX = 5;
    /** DB에서 한 번에 조회하는 콘텐츠 상한. 완료·추천이력 제외 후에도 RECOMMENDED_MAX를 채울 수 있는 여유값. */
    private static final int CONTENT_FETCH_LIMIT = 20;
    private static final String ONBOARDING_REDIRECT = "/onboarding/test";
    private static final String HOME_CACHE_PREFIX = "home:";
    private static final String REC_HISTORY_PREFIX = "home:rec_history:";
    private static final int LONG_ABSENCE_DAYS = 7;
    private static final long REC_HISTORY_TTL_DAYS = 7L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;
    private final ContentRepository contentRepository;
    private final ContentProgressRepository contentProgressRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 홈 화면 데이터를 반환한다.
     *
     * <p>온보딩 미완료 시 {@link OnboardingRequiredResponse}를 반환한다.
     * 캐시 히트 시 DB 조회 없이 즉시 응답한다.
     */
    @Transactional
    public Object getHome(String email) {
        User user = findUserByEmail(email);

        if (!user.isOnboardingCompleted()) {
            return new OnboardingRequiredResponse(true, ONBOARDING_REDIRECT);
        }

        String cacheKey = HOME_CACHE_PREFIX + user.getId();
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("홈 캐시 히트: userId={}", user.getId());
                return OBJECT_MAPPER.readValue(cachedJson, HomeResponse.class);
            }
        } catch (Exception e) {
            log.warn("홈 캐시 읽기 실패 (Redis 불가용), DB에서 조회합니다: userId={}", user.getId());
        }

        HomeResponse response = buildHomeResponse(user, email);
        user.updateLastVisitedAt();
        updateRecHistory(user.getId(), response.recommendedContents());

        try {
            Duration ttl = computeTtlUntilMidnight();
            String json = OBJECT_MAPPER.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(cacheKey, json, ttl);
            log.debug("홈 캐시 저장: userId={}, ttl={}s", user.getId(), ttl.getSeconds());
        } catch (Exception e) {
            log.warn("홈 캐시 저장 실패 (Redis 불가용): userId={}", user.getId());
        }

        return response;
    }

    // ── 홈 데이터 조립 ──────────────────────────────────────────────────────────

    private HomeResponse buildHomeResponse(User user, String email) {
        List<String> interestDomains = userInterestRepository.findDomainIdsByUserId(user.getId());
        Set<Long> completedIds = new HashSet<>(
            contentProgressRepository.findCompletedContentIdsByUserEmail(email));
        Set<Long> recentlyRecommendedIds = getRecentlyRecommendedIds(user.getId());
        Set<Long> excludedIds = new HashSet<>(completedIds);
        excludedIds.addAll(recentlyRecommendedIds);

        UserLevel level = user.getLevel() != null ? user.getLevel() : UserLevel.SEED;
        ContentDifficulty contentDiff = toContentDifficulty(level);

        // 콘텐츠 3종 사전 조회 — buildRecommended, buildTodayCard에서 공유하여 중복 쿼리 제거
        // CONTENT_FETCH_LIMIT로 DB 레벨 LIMIT를 걸어 대량 콘텐츠 전체 로드를 방지한다
        PageRequest contentPage = PageRequest.of(0, CONTENT_FETCH_LIMIT);
        List<Content> interestContents = interestDomains.isEmpty()
            ? List.of()
            : contentRepository.findByCategoriesInAndDifficultyPublished(interestDomains, contentDiff, contentPage);
        List<Content> diffContents = contentRepository.findByDifficultyPublished(contentDiff, contentPage);
        List<Content> allContents = contentRepository.findAllPublishedOrderByLastEdited(contentPage);

        List<RecommendedContentDto> recommended = buildRecommended(
            interestContents, diffContents, allContents, level, completedIds, excludedIds);
        TodayCardDto todayCard = buildTodayCard(
            user.getId(), interestContents, diffContents, allContents, completedIds);
        ContinueLearningDto continueLearning = buildContinueLearning(email);
        TodayQuizDto todayQuiz = buildTodayQuiz(user.getId(), level, email);
        List<CategoryDto> categories = buildCategories(email);

        LevelInfo levelInfo = LevelInfo.from(level);
        String levelImageKey = resolveLevelImageKey(user.getLevel());
        HomeUserDto userDto = new HomeUserDto(
            user.getNickname(),
            user.getUserType() != null ? user.getUserType().name() : null,
            level.name(),
            levelInfo.getLabel(),
            levelInfo.getEmoji(),
            levelImageKey
        );

        LastVisitDto lastVisit = buildLastVisit(user.getLastVisitedAt());
        WelcomeBackDto welcomeBack = buildWelcomeBack(user.getLastVisitedAt());

        return new HomeResponse(
            false, userDto, todayCard, recommended, continueLearning, todayQuiz, categories,
            lastVisit, welcomeBack
        );
    }

    // ── 추천 콘텐츠 ────────────────────────────────────────────────────────────

    private List<RecommendedContentDto> buildRecommended(
        List<Content> interestContents, List<Content> diffContents, List<Content> allContents,
        UserLevel level, Set<Long> completedIds, Set<Long> excludedIds) {

        Set<Long> addedIds = new LinkedHashSet<>();
        List<RecommendedContentDto> result = new ArrayList<>();

        if (!interestContents.isEmpty()) {
            interestContents.stream()
                .filter(c -> !excludedIds.contains(c.getId()))
                .limit(RECOMMENDED_MAX)
                .forEach(c -> {
                    addedIds.add(c.getId());
                    result.add(toRecommendedDto(c, false, false));
                });
        }

        if (result.size() < RECOMMENDED_MAX) {
            diffContents.stream()
                .filter(c -> !excludedIds.contains(c.getId()) && !addedIds.contains(c.getId()))
                .limit((long) RECOMMENDED_MAX - result.size())
                .forEach(c -> {
                    addedIds.add(c.getId());
                    result.add(toRecommendedDto(c, false, false));
                });
        }

        if (result.size() < RECOMMENDED_MAX) {
            allContents.stream()
                .filter(c -> !excludedIds.contains(c.getId()) && !addedIds.contains(c.getId()))
                .limit((long) RECOMMENDED_MAX - result.size())
                .forEach(c -> {
                    addedIds.add(c.getId());
                    result.add(toRecommendedDto(c, false, false));
                });
        }

        // 추천 소진 시 상위 레벨 미리보기
        if (result.size() < RECOMMENDED_MAX) {
            UserLevel nextLevel = nextLevel(level);
            if (nextLevel != null) {
                contentRepository.findByDifficultyPublished(
                        toContentDifficulty(nextLevel),
                        PageRequest.of(0, RECOMMENDED_MAX - result.size()))
                    .stream()
                    .filter(c -> !addedIds.contains(c.getId()))
                    .forEach(c -> result.add(toRecommendedDto(c, false, true)));
            }
        }

        return result;
    }

    private RecommendedContentDto toRecommendedDto(
        Content content, boolean completed, boolean isPreview) {
        String category = content.getFirstCategory();
        String emoji = ContentCategory.findById(category)
            .map(ContentCategory::getEmoji).orElse("");
        return new RecommendedContentDto(
            String.valueOf(content.getId()),
            content.getTitle(),
            category,
            emoji,
            difficultyLabel(content.getDifficulty()),
            0,
            completed,
            isPreview
        );
    }

    private UserLevel nextLevel(UserLevel level) {
        return switch (level) {
            case SEED -> UserLevel.SPROUT;
            case SPROUT -> UserLevel.TREE;
            case TREE -> null;
        };
    }

    // ── 오늘의 카드 ────────────────────────────────────────────────────────────

    private TodayCardDto buildTodayCard(
        Long userId, List<Content> interestContents, List<Content> diffContents,
        List<Content> allContents, Set<Long> completedIds) {

        List<Content> candidates = new ArrayList<>();

        interestContents.stream()
            .filter(c -> !completedIds.contains(c.getId()))
            .forEach(candidates::add);

        if (candidates.isEmpty()) {
            diffContents.stream()
                .filter(c -> !completedIds.contains(c.getId()))
                .forEach(candidates::add);
        }

        if (candidates.isEmpty()) {
            allContents.stream()
                .filter(c -> !completedIds.contains(c.getId()))
                .forEach(candidates::add);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        int index = deterministicIndex(userId, candidates.size());
        Content picked = candidates.get(index);
        String category = picked.getFirstCategory();
        String emoji = ContentCategory.findById(category)
            .map(ContentCategory::getEmoji).orElse("");

        return new TodayCardDto(
            String.valueOf(picked.getId()),
            picked.getTitle(),
            category,
            emoji,
            0
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
        Map<String, Long> publishedCounts = contentRepository.countPublishedGroupByCategory()
            .stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
            ));
        Map<String, Long> completedCounts =
            contentProgressRepository.countCompletedByUserEmailGroupByCategory(email)
                .stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
        return ContentCategory.all().stream()
            .map(cat -> new CategoryDto(
                cat.getNotionTag(),
                cat.getLabel(),
                cat.getEmoji(),
                publishedCounts.getOrDefault(cat.getNotionTag(), 0L),
                completedCounts.getOrDefault(cat.getNotionTag(), 0L)
            ))
            .toList();
    }

    // ── 마지막 접속 / 복귀 메시지 ────────────────────────────────────────────────

    private LastVisitDto buildLastVisit(LocalDateTime lastVisitedAt) {
        if (lastVisitedAt == null) {
            return new LastVisitDto(null, 0);
        }
        int days = (int) ChronoUnit.DAYS.between(lastVisitedAt.toLocalDate(), LocalDate.now());
        return new LastVisitDto(lastVisitedAt, days);
    }

    private WelcomeBackDto buildWelcomeBack(LocalDateTime lastVisitedAt) {
        if (lastVisitedAt == null) {
            return null;
        }
        int days = (int) ChronoUnit.DAYS.between(lastVisitedAt.toLocalDate(), LocalDate.now());
        if (days < LONG_ABSENCE_DAYS) {
            return null;
        }
        return new WelcomeBackDto(true, days);
    }

    // ── 추천 이력 (Redis) ───────────────────────────────────────────────────────

    private Set<Long> getRecentlyRecommendedIds(Long userId) {
        try {
            String key = REC_HISTORY_PREFIX + userId;
            String raw = stringRedisTemplate.opsForValue().get(key);
            if (raw != null) {
                return OBJECT_MAPPER.readValue(raw, new TypeReference<Set<Long>>() {});
            }
        } catch (Exception e) {
            log.warn("추천 이력 읽기 실패 (Redis 불가용): userId={}", userId);
        }
        return new HashSet<>();
    }

    private void updateRecHistory(Long userId, List<RecommendedContentDto> contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }
        try {
            String key = REC_HISTORY_PREFIX + userId;
            Set<Long> ids = contents.stream()
                .filter(c -> !c.isPreview())
                .map(c -> Long.parseLong(c.id()))
                .collect(Collectors.toSet());

            Set<Long> existing = getRecentlyRecommendedIds(userId);
            existing.addAll(ids);
            String json = OBJECT_MAPPER.writeValueAsString(existing);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofDays(REC_HISTORY_TTL_DAYS));
        } catch (Exception e) {
            log.warn("추천 이력 저장 실패 (Redis 불가용): userId={}", userId);
        }
    }

    // ── 유틸리티 ────────────────────────────────────────────────────────────────

    private Duration computeTtlUntilMidnight() {
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), midnight);
        return Duration.ofSeconds(Math.max(seconds, 1L));
    }

    private int deterministicIndex(Long userId, int size) {
        long seed = (userId != null ? userId : 0L) + LocalDate.now().toEpochDay();
        return (int) Math.abs(seed % size);
    }

    private static String difficultyLabel(ContentDifficulty difficulty) {
        if (difficulty == null) {
            return "";
        }
        return switch (difficulty) {
            case SEED -> "왕초보";
            case SPROUT -> "초보";
            case TREE -> "중급";
        };
    }

    private static ContentDifficulty toContentDifficulty(UserLevel level) {
        if (level == null) {
            return ContentDifficulty.SEED;
        }
        return ContentDifficulty.valueOf(level.name());
    }

    private String resolveLevelImageKey(UserLevel level) {
        if (level == null) {
            return "default";
        }
        return switch (level) {
            case SEED -> "seed";
            case SPROUT -> "sprout";
            case TREE -> "tree";
        };
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}

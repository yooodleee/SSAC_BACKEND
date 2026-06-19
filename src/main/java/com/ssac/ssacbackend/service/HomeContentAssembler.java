package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentCategory;
import com.ssac.ssacbackend.domain.content.ContentDifficulty;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.HomeResponse.CategoryDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.ContinueLearningDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.RecommendedContentDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.TodayCardDto;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 홈 화면 콘텐츠 섹션 조립기.
 *
 * <p>추천 콘텐츠·오늘의 카드·이어보기·카테고리 섹션을 빌드한다.
 * {@link HomeService}에서 위임받아 실행되며, 캐시 및 사용자 조회 로직은 HomeService가 담당한다.
 */
@Service
@RequiredArgsConstructor
public class HomeContentAssembler {

    private static final int RECOMMENDED_MAX = 5;
    /** DB에서 한 번에 조회하는 콘텐츠 상한. 완료·추천이력 제외 후에도 RECOMMENDED_MAX를 채울 수 있는 여유값. */
    private static final int CONTENT_FETCH_LIMIT = 20;

    private final UserInterestRepository userInterestRepository;
    private final ContentRepository contentRepository;
    private final ContentProgressRepository contentProgressRepository;

    /**
     * 홈 콘텐츠 섹션 전체를 빌드하여 반환한다.
     *
     * @param userId                  사용자 ID (오늘의 카드 결정론적 선택에 사용)
     * @param email                   사용자 이메일
     * @param level                   사용자 레벨
     * @param recentlyRecommendedIds  최근 추천 이력 (추천 중복 방지)
     */
    public ContentSections build(Long userId, String email, UserLevel level,
                                 Set<Long> recentlyRecommendedIds) {
        List<String> interestDomains = userInterestRepository.findDomainIdsByUserId(userId);
        Set<Long> completedIds = new HashSet<>(
            contentProgressRepository.findCompletedContentIdsByUserEmail(email));
        Set<Long> excludedIds = new HashSet<>(completedIds);
        excludedIds.addAll(recentlyRecommendedIds);

        ContentDifficulty contentDiff = toContentDifficulty(level);
        PageRequest contentPage = PageRequest.of(0, CONTENT_FETCH_LIMIT);

        List<Content> interestContents = interestDomains.isEmpty()
            ? List.of()
            : contentRepository.findByCategoriesInAndDifficultyPublished(
                interestDomains, contentDiff, contentPage);
        List<Content> diffContents = contentRepository.findByDifficultyPublished(contentDiff, contentPage);
        List<Content> allContents = contentRepository.findAllPublishedOrderByLastEdited(contentPage);

        List<RecommendedContentDto> recommended = buildRecommended(
            interestContents, diffContents, allContents, level, completedIds, excludedIds);
        TodayCardDto todayCard = buildTodayCard(
            userId, interestContents, diffContents, allContents, completedIds);
        ContinueLearningDto continueLearning = buildContinueLearning(email);
        List<CategoryDto> categories = buildCategories(email);

        return new ContentSections(recommended, todayCard, continueLearning, categories);
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

    // ── 유틸리티 ────────────────────────────────────────────────────────────────

    private static ContentDifficulty toContentDifficulty(UserLevel level) {
        if (level == null) {
            return ContentDifficulty.SEED;
        }
        return ContentDifficulty.valueOf(level.name());
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

    private static int deterministicIndex(Long userId, int size) {
        long seed = (userId != null ? userId : 0L) + LocalDate.now().toEpochDay();
        return (int) Math.abs(seed % size);
    }

    // ── 반환 타입 ────────────────────────────────────────────────────────────────

    public record ContentSections(
        List<RecommendedContentDto> recommended,
        TodayCardDto todayCard,
        ContinueLearningDto continueLearning,
        List<CategoryDto> categories
    ) {}
}

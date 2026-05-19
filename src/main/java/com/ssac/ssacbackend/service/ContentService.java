package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentProgress;
import com.ssac.ssacbackend.domain.content.ContentViewHistory;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.ContentCompleteResponse;
import com.ssac.ssacbackend.dto.response.ContentItemDto;
import com.ssac.ssacbackend.dto.response.ContentListResponse;
import com.ssac.ssacbackend.dto.response.LevelUpResult;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.ContentViewHistoryRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 콘텐츠 레벨/카테고리 필터링 및 완료 처리 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ContentProgressRepository contentProgressRepository;
    private final LevelUpService levelUpService;
    private final HomeCacheEvictService homeCacheEvictService;
    private final ContentViewHistoryRepository contentViewHistoryRepository;

    /**
     * 레벨/카테고리 기준 콘텐츠 목록을 반환한다.
     *
     * <p>level 미지정 시 사용자의 현재 레벨을 사용한다.
     *
     * @param email    사용자 이메일
     * @param levelStr 레벨 파라미터 (null 허용)
     * @param category 카테고리 파라미터 (null 허용)
     */
    @Transactional(readOnly = true)
    public ContentListResponse getContents(String email, String levelStr, String category) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        UserLevel level = resolveLevel(levelStr, user);

        Set<Long> completedIds = new HashSet<>(
            contentProgressRepository.findCompletedContentIdsByUserEmail(email));

        List<Content> contents = fetchContents(level, category);

        List<ContentItemDto> items = contents.stream()
            .map(c -> toItemDto(c, completedIds.contains(c.getId())))
            .toList();

        return new ContentListResponse(
            level.name(),
            category,
            items.size(),
            items
        );
    }

    /**
     * 콘텐츠 학습을 완료 처리하고 레벨업 조건을 검사한다.
     *
     * @param email     사용자 이메일
     * @param contentId 완료할 콘텐츠 ID
     * @return 완료 처리 결과 (레벨업 여부 포함)
     */
    @Transactional
    public ContentCompleteResponse complete(String email, Long contentId) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        Content content = contentRepository.findById(contentId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.CONTENT_NOT_FOUND));

        Optional<ContentProgress> existing =
            contentProgressRepository.findByContentIdAndUserId(contentId, user.getId());

        if (existing.isPresent()) {
            existing.get().updateProgress("complete", ContentProgress.COMPLETION_THRESHOLD);
        } else {
            contentProgressRepository.save(
                ContentProgress.builder()
                    .user(user)
                    .title(content.getTitle())
                    .lastPosition("complete")
                    .progressRate(ContentProgress.COMPLETION_THRESHOLD)
                    .contentId(content.getId())
                    .category(content.getCategory())
                    .build()
            );
        }
        content.incrementViewCount();

        contentViewHistoryRepository.findByUserIdOrderByViewedAtDesc(user.getId())
            .stream()
            .filter(h -> h.getContentId().equals(contentId) && !h.isCompleted())
            .findFirst()
            .ifPresent(ContentViewHistory::markCompleted);

        LevelUpResult levelUpResult = levelUpService.checkAndApplyLevelUp(user, email);
        log.info("콘텐츠 완료: email={}, contentId={}, levelUp={}", email, contentId,
            levelUpResult.leveledUp());
        homeCacheEvictService.evict(user.getId());

        return new ContentCompleteResponse(
            String.valueOf(contentId),
            true,
            levelUpResult.leveledUp(),
            levelUpResult.previousLevel() != null ? levelUpResult.previousLevel().name() : null,
            levelUpResult.newLevel() != null ? levelUpResult.newLevel().name() : null
        );
    }

    /**
     * 콘텐츠 조회 이력을 저장한다. 콘텐츠 열람 시 호출한다.
     *
     * @param email     사용자 이메일
     * @param contentId 콘텐츠 ID
     */
    @Transactional
    public void recordView(String email, Long contentId) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        ContentViewHistory history = ContentViewHistory.of(user.getId(), contentId);
        contentViewHistoryRepository.save(history);
        log.debug("콘텐츠 조회 이력 저장: userId={}, contentId={}", user.getId(), contentId);
    }

    private UserLevel resolveLevel(String levelStr, User user) {
        if (levelStr == null || levelStr.isBlank()) {
            return user.getLevel() != null ? user.getLevel() : UserLevel.SEED;
        }
        try {
            return UserLevel.valueOf(levelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return user.getLevel() != null ? user.getLevel() : UserLevel.SEED;
        }
    }

    private List<Content> fetchContents(UserLevel level, String category) {
        if (category != null && !category.isBlank()) {
            return contentRepository.findByCategoryAndDifficultyOrderByViewCountDesc(category, level);
        }
        return contentRepository.findByDifficultyOrderByViewCountDesc(level);
    }

    private ContentItemDto toItemDto(Content content, boolean completed) {
        return new ContentItemDto(
            String.valueOf(content.getId()),
            content.getTitle(),
            content.getDifficulty() != null ? content.getDifficulty().name() : null,
            difficultyLabel(content.getDifficulty()),
            content.getEstimatedMinutes(),
            completed,
            content.getViewCount()
        );
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
}

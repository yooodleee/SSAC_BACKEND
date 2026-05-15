package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.ContentItemDto;
import com.ssac.ssacbackend.dto.response.ContentListResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 콘텐츠 레벨/카테고리 필터링 서비스.
 */
@Service
@RequiredArgsConstructor
public class ContentService {

    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ContentProgressRepository contentProgressRepository;

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

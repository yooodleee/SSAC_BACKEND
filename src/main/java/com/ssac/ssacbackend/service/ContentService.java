package com.ssac.ssacbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentProgress;
import com.ssac.ssacbackend.domain.content.ContentViewHistory;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.response.ContentCompleteResponse;
import com.ssac.ssacbackend.dto.response.ContentDetailResponse;
import com.ssac.ssacbackend.dto.response.ContentItemDto;
import com.ssac.ssacbackend.dto.response.ContentListResponse;
import com.ssac.ssacbackend.dto.response.LevelUpResult;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.ContentViewHistoryRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.blocks.Blocks;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 콘텐츠 목록 조회 및 완료 처리 서비스.
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
    private final NotionSyncService notionSyncService;
    private final NotionClient notionClient;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 게시된 콘텐츠 목록을 반환한다. 비로그인 사용자도 조회 가능하다.
     *
     * @param auth       인증 정보 (null 또는 AnonymousAuthenticationToken이면 비로그인)
     * @param category   카테고리 필터 (null 허용)
     * @param difficulty 난이도 필터 (null 허용)
     * @param domain     도메인 필터 (null 허용)
     */
    @Transactional(readOnly = true)
    public ContentListResponse getContents(
        Authentication auth, String category, String difficulty, String domain) {

        List<ContentItemDto> cachedItems =
            notionSyncService.getPublishedContentItems(category, difficulty, domain);

        if (!isAuthenticated(auth)) {
            return new ContentListResponse(cachedItems.size(), cachedItems);
        }

        Set<Long> completedIds = new HashSet<>(
            contentProgressRepository.findCompletedContentIdsByUserEmail(auth.getName()));

        List<ContentItemDto> items = cachedItems.stream()
            .map(item -> completedIds.contains(Long.parseLong(item.id()))
                ? new ContentItemDto(item.id(), item.title(), item.thumbnailUrl(),
                    item.categories(), item.domains(), item.difficulty(),
                    item.difficultyLabel(), true, item.publishedAt())
                : item)
            .toList();

        return new ContentListResponse(items.size(), items);
    }

    /**
     * 콘텐츠 상세를 반환한다. Notion 블록을 실시간으로 조회한다.
     *
     * @param id   콘텐츠 ID
     * @param auth 인증 정보 (null 허용)
     * @throws NotFoundException 존재하지 않는 콘텐츠
     */
    @Transactional(readOnly = true)
    public ContentDetailResponse getContent(Long id, Authentication auth) {
        Content content = contentRepository.findById(id)
            .filter(Content::isPublished)
            .orElseThrow(() -> new NotFoundException(ErrorCode.CONTENT_NOT_FOUND));

        List<Map<String, Object>> blocks = fetchNotionBlocks(content.getNotionPageId());

        return new ContentDetailResponse(
            String.valueOf(content.getId()),
            content.getNotionPageId(),
            content.getTitle(),
            content.getThumbnailUrl(),
            List.copyOf(content.getCategories()),
            List.copyOf(content.getDomains()),
            content.getDifficulty() != null ? content.getDifficulty().name() : null,
            NotionSyncService.difficultyLabel(content.getDifficulty()),
            content.getNotionCreatedAt(),
            content.getNotionLastEditedAt(),
            blocks
        );
    }

    /**
     * 콘텐츠 학습을 완료 처리하고 레벨업 조건을 검사한다.
     */
    @Transactional
    public ContentCompleteResponse complete(String email, Long contentId) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        Content content = contentRepository.findById(contentId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.CONTENT_NOT_FOUND));

        Optional<ContentProgress> existing =
            contentProgressRepository.findByContentIdAndUserId(contentId, user.getId());

        String firstCategory = content.getFirstCategory();

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
                    .category(firstCategory)
                    .build()
            );
        }

        contentViewHistoryRepository.findByUserIdOrderByViewedAtDesc(user.getId())
            .stream()
            .filter(h -> h.getContentId().equals(contentId) && !h.isCompleted())
            .findFirst()
            .ifPresent(ContentViewHistory::markCompleted);

        LevelUpResult levelUpResult = levelUpService.checkAndApplyLevelUp(user, email);
        log.info("콘텐츠 완료: email={}, contentId={}, levelUp={}",
            email, contentId, levelUpResult.leveledUp());
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
     * 콘텐츠 조회 이력을 저장한다. 로그인 사용자 전용.
     */
    @Transactional
    public void recordView(String email, Long contentId) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        ContentViewHistory history = ContentViewHistory.of(user.getId(), contentId);
        contentViewHistoryRepository.save(history);
        log.debug("콘텐츠 조회 이력 저장: userId={}, contentId={}", user.getId(), contentId);
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchNotionBlocks(String notionPageId) {
        if (notionPageId == null || notionPageId.isBlank()) {
            return List.of();
        }
        try {
            Blocks blocks = notionClient.retrieveBlockChildren(notionPageId, null, 100);
            return blocks.getResults().stream()
                .<Map<String, Object>>map(block -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = OBJECT_MAPPER.convertValue(block, Map.class);
                        return map;
                    } catch (Exception e) {
                        log.warn("블록 직렬화 실패: blockId={}", block.getId(), e);
                        java.util.Map<String, Object> fallback = new java.util.HashMap<>();
                        fallback.put("id", block.getId());
                        fallback.put("type", block.getType());
                        return fallback;
                    }
                })
                .toList();
        } catch (Exception e) {
            log.warn("Notion 블록 조회 실패: notionPageId={}", notionPageId, e);
            return List.of();
        }
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated()
            && !(auth instanceof AnonymousAuthenticationToken);
    }
}

package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentCategory;
import com.ssac.ssacbackend.domain.search.SearchKeyword;
import com.ssac.ssacbackend.dto.response.SearchResultResponse;
import com.ssac.ssacbackend.dto.response.SearchResultResponse.SearchItem;
import com.ssac.ssacbackend.dto.response.SearchSuggestionResponse;
import com.ssac.ssacbackend.dto.response.SearchSuggestionResponse.SuggestionItem;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.SearchKeywordRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 검색 서비스.
 *
 * <p>콘텐츠 검색 / 자동완성 / 인기 검색어 집계를 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int SUGGESTION_LIMIT = 10;

    private static final Map<String, String> DIFFICULTY_LABEL = Map.of(
        "SEED", "씨앗",
        "SPROUT", "새싹",
        "TREE", "나무"
    );

    private final ContentRepository contentRepository;
    private final SearchKeywordRepository searchKeywordRepository;

    /**
     * 검색어 자동완성 — 빈 쿼리 시 인기 검색어, 아닌 경우 콘텐츠 제목 기반 결과 반환.
     */
    @Transactional(readOnly = true)
    public SearchSuggestionResponse getSuggestions(String query) {
        if (query == null || query.isBlank()) {
            return getPopularSuggestions();
        }
        return getContentSuggestions(query.trim());
    }

    private SearchSuggestionResponse getPopularSuggestions() {
        List<SearchKeyword> keywords = searchKeywordRepository.findTop10ByOrderBySearchCountDesc();
        List<SuggestionItem> items = keywords.stream()
            .map(k -> new SuggestionItem(null, k.getKeyword(), null, null, k.getSearchCount()))
            .toList();
        return new SearchSuggestionResponse("", items, items.size());
    }

    private SearchSuggestionResponse getContentSuggestions(String query) {
        // Slice 사용 — COUNT 쿼리 없이 LIMIT만 적용하여 자동완성 성능 개선
        List<Content> contents = contentRepository
            .findSuggestionsByTitleContaining(query, PageRequest.of(0, SUGGESTION_LIMIT))
            .getContent();

        List<SuggestionItem> items = contents.stream()
            .map(c -> {
                String category = c.getFirstCategory();
                return new SuggestionItem(
                    String.valueOf(c.getId()),
                    c.getTitle(),
                    category,
                    getCategoryEmoji(category),
                    0L
                );
            })
            .toList();

        return new SearchSuggestionResponse(query, items, items.size());
    }

    /**
     * 콘텐츠 검색 — 빈 쿼리 시 400 응답.
     */
    @Transactional
    public SearchResultResponse search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException(ErrorCode.SEARCH_QUERY_REQUIRED);
        }

        String trimmed = query.trim();
        trackSearchKeyword(trimmed);

        Page<Content> contentPage = contentRepository.findByIsPublishedTrueAndTitleContainingPaged(
            trimmed, PageRequest.of(page - 1, size));

        List<SearchItem> results = contentPage.getContent().stream()
            .map(c -> toSearchItem(c, trimmed))
            .toList();

        return new SearchResultResponse(
            trimmed,
            contentPage.getTotalElements(),
            page,
            size,
            results
        );
    }

    private void trackSearchKeyword(String keyword) {
        searchKeywordRepository.findByKeyword(keyword)
            .ifPresentOrElse(
                SearchKeyword::incrementCount,
                () -> searchKeywordRepository.save(SearchKeyword.create(keyword))
            );
    }

    private SearchItem toSearchItem(Content c, String query) {
        String difficultyName = c.getDifficulty() != null ? c.getDifficulty().name() : null;
        String difficultyLabel = difficultyName != null ? DIFFICULTY_LABEL.get(difficultyName) : null;
        String category = c.getFirstCategory();
        return new SearchItem(
            String.valueOf(c.getId()),
            c.getTitle(),
            category,
            getCategoryEmoji(category),
            difficultyName,
            difficultyLabel,
            0,
            buildHighlightedTitle(c.getTitle() != null ? c.getTitle() : "", query)
        );
    }

    private String buildHighlightedTitle(String title, String query) {
        return title.replace(query, "**" + query + "**");
    }

    private String getCategoryEmoji(String categoryName) {
        if (categoryName == null) {
            return null;
        }
        return ContentCategory.findByName(categoryName)
            .map(ContentCategory::getEmoji)
            .orElse(null);
    }
}

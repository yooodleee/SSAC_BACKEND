package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.search.SearchKeyword;
import com.ssac.ssacbackend.dto.response.SearchResultResponse;
import com.ssac.ssacbackend.dto.response.SearchSuggestionResponse;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.SearchKeywordRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private SearchKeywordRepository searchKeywordRepository;

    @InjectMocks
    private SearchService searchService;

    // ── 자동완성 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("검색어 입력 시 자동완성 결과가 가나다 순으로 응답된다")
    void 검색어_자동완성_가나다순() {
        Content c1 = makeContent(1L, "재테크 기초", "재테크/신용", 100);
        Content c2 = makeContent(2L, "개인연금 가이드", "재테크/신용", 200);
        given(contentRepository.findByIsPublishedTrueAndTitleContaining("연"))
            .willReturn(List.of(c2, c1));

        SearchSuggestionResponse result = searchService.getSuggestions("연");

        assertThat(result.query()).isEqualTo("연");
        assertThat(result.suggestions()).hasSize(2);
        assertThat(result.suggestions().get(0).keyword()).isEqualTo("개인연금 가이드");
    }

    @Test
    @DisplayName("초성 'ㄱ' 입력 시 ㄱ으로 시작하는 콘텐츠가 응답된다")
    void 초성_ㄱ_검색() {
        Content c1 = makeContent(1L, "개인연금", "재테크/신용", 100);
        Content c2 = makeContent(2L, "근로장려금", "재테크/신용", 200);
        given(contentRepository.findByIsPublishedTrueAndTitleContaining("ㄱ"))
            .willReturn(List.of(c1, c2));

        SearchSuggestionResponse result = searchService.getSuggestions("ㄱ");

        assertThat(result.query()).isEqualTo("ㄱ");
        assertThat(result.suggestions()).hasSize(2);
        assertThat(result.suggestions().get(0).keyword()).isEqualTo("개인연금");
    }

    @Test
    @DisplayName("빈 검색어 입력 시 인기 검색어 최대 10개가 응답된다")
    void 빈_검색어_인기검색어_10개() {
        List<SearchKeyword> keywords = IntStream.rangeClosed(1, 10)
            .mapToObj(i -> makeSearchKeyword("키워드" + i, 100 - i))
            .toList();
        given(searchKeywordRepository.findTop10ByOrderBySearchCountDesc()).willReturn(keywords);

        SearchSuggestionResponse result = searchService.getSuggestions("");

        assertThat(result.query()).isEqualTo("");
        assertThat(result.suggestions()).hasSize(10);
        assertThat(result.totalCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("자동완성 결과는 최대 10개로 제한된다")
    void 자동완성_최대_10개_제한() {
        List<Content> manyContents = IntStream.rangeClosed(1, 15)
            .mapToObj(i -> makeContent((long) i, "연말정산" + i, "세금/연말정산", i * 10))
            .toList();
        given(contentRepository.findByIsPublishedTrueAndTitleContaining("연말"))
            .willReturn(manyContents);

        SearchSuggestionResponse result = searchService.getSuggestions("연말");

        assertThat(result.suggestions()).hasSize(10);
        assertThat(result.totalCount()).isEqualTo(10);
    }

    // ── 콘텐츠 검색 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("검색 결과가 있는 경우 정상 응답된다")
    void 검색_결과_있음() {
        Content c = makeContent(1L, "연말정산 공제 항목", "세금/연말정산", 50);
        given(contentRepository.findByIsPublishedTrueAndTitleContainingPaged(eq("연말정산"), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(c)));
        given(searchKeywordRepository.findByKeyword("연말정산")).willReturn(Optional.empty());

        SearchResultResponse result = searchService.search("연말정산", 1, 20);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).title()).isEqualTo("연말정산 공제 항목");
        assertThat(result.results().get(0).highlightedTitle())
            .isEqualTo("**연말정산** 공제 항목");
    }

    @Test
    @DisplayName("검색 결과가 없는 경우 totalCount 0으로 응답된다")
    void 검색_결과_없음() {
        given(contentRepository.findByIsPublishedTrueAndTitleContainingPaged(eq("없는키워드"), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of()));
        given(searchKeywordRepository.findByKeyword("없는키워드")).willReturn(Optional.empty());

        SearchResultResponse result = searchService.search("없는키워드", 1, 20);

        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.results()).isEmpty();
    }

    @Test
    @DisplayName("검색 제출 시 기존 키워드의 searchCount가 증가한다")
    void 검색_제출_searchCount_증가() {
        SearchKeyword keyword = makeSearchKeyword("연말정산", 5);
        given(contentRepository.findByIsPublishedTrueAndTitleContainingPaged(eq("연말정산"), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of()));
        given(searchKeywordRepository.findByKeyword("연말정산")).willReturn(Optional.of(keyword));

        searchService.search("연말정산", 1, 20);

        assertThat(keyword.getSearchCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("빈 검색어 제출 시 400 / SEARCH-001 응답된다")
    void 빈_검색어_400_SEARCH_001() {
        assertThatThrownBy(() -> searchService.search("", 1, 20))
            .isInstanceOf(BadRequestException.class)
            .satisfies(e -> assertThat(((BadRequestException) e).getErrorCode())
                .isEqualTo(ErrorCode.SEARCH_QUERY_REQUIRED));

        verify(contentRepository, never())
            .findByIsPublishedTrueAndTitleContainingPaged(any(), any());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Content makeContent(Long id, String title, String category, long viewCount) {
        Content c = Content.fromNotion("page-" + id, "db-id");
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "title", title);
        ReflectionTestUtils.setField(c, "categories", List.of(category));
        ReflectionTestUtils.setField(c, "domains", new LinkedHashSet<>());
        return c;
    }

    private SearchKeyword makeSearchKeyword(String keyword, long count) {
        SearchKeyword sk = SearchKeyword.create(keyword);
        ReflectionTestUtils.setField(sk, "searchCount", count);
        return sk;
    }
}

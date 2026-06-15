package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.SearchResultResponse;
import com.ssac.ssacbackend.dto.response.SearchSuggestionResponse;
import com.ssac.ssacbackend.service.SearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("SearchController")
class SearchControllerTest {

    private SearchService searchService;
    private SearchController controller;

    @BeforeEach
    void setUp() {
        searchService = mock(SearchService.class);
        controller = new SearchController(searchService);
    }

    // ── getSuggestions ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSuggestions - 검색어 자동완성")
    class GetSuggestions {

        @Test
        @DisplayName("검색어 입력 시 자동완성 결과 200을 반환한다")
        void getSuggestions_검색어있음() {
            SearchSuggestionResponse mockResponse =
                new SearchSuggestionResponse("스프링", List.of(), 0);
            given(searchService.getSuggestions("스프링")).willReturn(mockResponse);

            ResponseEntity<ApiResponse<SearchSuggestionResponse>> result =
                controller.getSuggestions("스프링");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("빈 검색어 입력 시 인기 검색어를 반환한다")
        void getSuggestions_검색어없음() {
            SearchSuggestionResponse mockResponse =
                new SearchSuggestionResponse("", List.of(), 0);
            given(searchService.getSuggestions("")).willReturn(mockResponse);

            ResponseEntity<ApiResponse<SearchSuggestionResponse>> result =
                controller.getSuggestions("");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(searchService).getSuggestions("");
        }
    }

    // ── search ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("search - 콘텐츠 검색")
    class Search {

        @Test
        @DisplayName("검색어로 검색 성공 시 200과 결과를 반환한다")
        void search_성공() {
            SearchResultResponse mockResponse =
                new SearchResultResponse("스프링", 3L, 1, 20, List.of());
            given(searchService.search("스프링", 1, 20)).willReturn(mockResponse);

            ResponseEntity<ApiResponse<SearchResultResponse>> result =
                controller.search("스프링", 1, 20);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData()).isEqualTo(mockResponse);
        }

        @Test
        @DisplayName("검색 시 올바른 인자를 서비스에 전달한다")
        void search_인자전달() {
            SearchResultResponse mockResponse =
                new SearchResultResponse("세금", 1L, 2, 10, List.of());
            given(searchService.search("세금", 2, 10)).willReturn(mockResponse);

            controller.search("세금", 2, 10);

            verify(searchService).search("세금", 2, 10);
        }
    }
}

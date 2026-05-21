package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.SearchResultResponse;
import com.ssac.ssacbackend.dto.response.SearchSuggestionResponse;
import com.ssac.ssacbackend.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 콘텐츠 검색 API.
 *
 * <p>자동완성 / 콘텐츠 검색 / 인기 검색어 제공.
 */
@Tag(name = "Search", description = "콘텐츠 검색 API")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(
        summary = "검색어 자동완성",
        description = """
            [호출 화면] 검색 입력 중 실시간 자동완성.
            [권한 조건] 공개 (로그인 불필요).
            [특이 동작] q가 비어 있으면 인기 검색어 10개를 반환한다.
                        초성 검색 지원 (예: ㄱ → ㄱ으로 시작하는 콘텐츠).
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "자동완성 결과 반환 (결과 없으면 빈 배열)")
    })
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<SearchSuggestionResponse>> getSuggestions(
        @Parameter(description = "검색어 (비어있으면 인기 검색어 반환)")
        @RequestParam(required = false, defaultValue = "") String q
    ) {
        return ResponseEntity.ok(ApiResponse.success(searchService.getSuggestions(q)));
    }

    @Operation(
        summary = "콘텐츠 검색",
        description = """
            [호출 화면] 검색 결과 화면.
            [권한 조건] 공개 (로그인 불필요).
            [특이 동작] 검색어 제출 시 인기 검색어 집계가 누적된다.
                        빈 검색어 요청 시 400 / SEARCH-001.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "검색 결과 반환"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "SEARCH-001: 검색어 미입력")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<SearchResultResponse>> search(
        @Parameter(description = "검색어", required = true)
        @RequestParam String q,
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(searchService.search(q, page, size)));
    }
}

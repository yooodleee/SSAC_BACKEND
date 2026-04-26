package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.request.NewsSortType;
import com.ssac.ssacbackend.dto.response.NewsListResponse;
import com.ssac.ssacbackend.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 뉴스 목록 API.
 *
 * <p>인증 불필요 — 비로그인 사용자도 조회 가능하다.
 */
@Slf4j
@Tag(name = "News", description = "뉴스 목록 조회 API")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @Operation(
        summary = "뉴스 목록 조회",
        description = """
            sort: latest(기본값) | popularity
            page: 1-based 페이지 번호 (기본값 1)
            size: 페이지 크기 (기본값 20, 최대 100)
            """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<NewsListResponse>> getNews(
        @Parameter(description = "정렬 기준 (latest | popularity)", example = "latest")
        @RequestParam(defaultValue = "latest") String sort,

        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") int page,

        @Parameter(description = "페이지 크기 (최대 100)", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        NewsSortType sortType = parseSortType(sort);
        if (page < 1) {
            throw BusinessException.badRequest("page는 1 이상이어야 합니다.");
        }
        log.debug("뉴스 목록 조회: sort={}, page={}, size={}", sort, page, size);
        return ResponseEntity.ok(ApiResponse.success(newsService.getNews(sortType, page, size)));
    }

    private NewsSortType parseSortType(String sort) {
        try {
            return NewsSortType.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalidSortParameter();
        }
    }
}

package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.dto.request.NewsSortType;
import com.ssac.ssacbackend.dto.response.NewsItemResponse;
import com.ssac.ssacbackend.dto.response.NewsListResponse;
import com.ssac.ssacbackend.repository.NewsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 뉴스 목록 조회 서비스.
 *
 * <p>정렬 기준:
 * <ul>
 *   <li>{@link NewsSortType#LATEST} - publishedAt DESC</li>
 *   <li>{@link NewsSortType#POPULARITY} - viewCount DESC, publishedAt DESC
 *       (viewCount는 최근 7일 이내 누적 조회 수 기준)</li>
 * </ul>
 *
 * <p>페이지네이션:
 * <ul>
 *   <li>page는 1부터 시작 (1-based)</li>
 *   <li>size 최댓값: {@value #MAX_PAGE_SIZE}</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class NewsService {

    static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NewsRepository newsRepository;

    @Transactional(readOnly = true)
    public NewsListResponse getNews(NewsSortType sortType, int page, int size) {
        if (size > MAX_PAGE_SIZE) {
            throw BusinessException.pageSizeExceeded(MAX_PAGE_SIZE);
        }

        Pageable pageable = PageRequest.of(page - 1, size, buildSort(sortType));
        Page<NewsItemResponse> result = newsRepository.findAll(pageable)
            .map(NewsItemResponse::from);

        List<NewsItemResponse> contents = result.getContent();
        return new NewsListResponse(result.getTotalElements(), result.hasNext(), contents);
    }

    private Sort buildSort(NewsSortType sortType) {
        if (sortType == NewsSortType.POPULARITY) {
            return Sort.by(
                Sort.Order.desc("viewCount"),
                Sort.Order.desc("publishedAt")
            );
        }
        return Sort.by(Sort.Order.desc("publishedAt"));
    }
}

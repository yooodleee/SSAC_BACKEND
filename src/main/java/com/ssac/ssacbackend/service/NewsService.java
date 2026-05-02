package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.news.News;
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
 *
 * <p>조회 이벤트 기록은 {@link ViewCountStore} 인터페이스에만 의존하므로
 * Redis 전환 시 이 클래스의 수정 없이 구현체 Bean 교체만으로 동작한다.
 */
@Service
@RequiredArgsConstructor
public class NewsService {

    static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NewsRepository newsRepository;
    private final ViewCountStore viewCountStore;

    /**
     * 뉴스 상세 조회 및 조회 이벤트 기록.
     *
     * <p>조회마다 {@link ViewCountStore#record}를 호출하며,
     * 이 데이터는 배치 집계로 viewCount에 반영된다.
     *
     * @param newsId 조회할 뉴스 ID
     * @throws BusinessException 뉴스가 존재하지 않을 경우 NOT_FOUND
     */
    @Transactional
    public NewsItemResponse getNewsDetail(Long newsId) {
        News news = newsRepository.findById(newsId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.NEWS_NOT_FOUND));
        viewCountStore.record(news);
        return NewsItemResponse.from(news);
    }

    @Transactional(readOnly = true)
    public NewsListResponse getNews(NewsSortType sortType, int page, int size) {
        if (size > MAX_PAGE_SIZE) {
            throw new BadRequestException(ErrorCode.PAGE_SIZE_EXCEEDED, "size는 최대 " + MAX_PAGE_SIZE + "까지 허용됩니다.");
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

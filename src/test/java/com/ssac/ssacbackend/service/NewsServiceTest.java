package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.domain.news.News;
import com.ssac.ssacbackend.domain.news.NewsView;
import com.ssac.ssacbackend.dto.request.NewsSortType;
import com.ssac.ssacbackend.dto.response.NewsItemResponse;
import com.ssac.ssacbackend.dto.response.NewsListResponse;
import com.ssac.ssacbackend.repository.NewsRepository;
import com.ssac.ssacbackend.repository.NewsViewRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;
    @Mock
    private NewsViewRepository newsViewRepository;

    @InjectMocks
    private NewsService newsService;

    // ── 목록 조회 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("최신순 정렬로 뉴스 목록을 조회하면 publishedAt 내림차순으로 요청한다")
    void 최신순_정렬_뉴스_목록_조회() {
        News news = buildNews(1L, "최신 뉴스", 10);
        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("publishedAt")));
        given(newsRepository.findAll(expectedPageable))
            .willReturn(new PageImpl<>(List.of(news)));

        NewsListResponse response = newsService.getNews(NewsSortType.LATEST, 1, 20);

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).title()).isEqualTo("최신 뉴스");
    }

    @Test
    @DisplayName("인기순 정렬로 뉴스 목록을 조회하면 viewCount 내림차순으로 요청한다")
    void 인기순_정렬_뉴스_목록_조회() {
        News news = buildNews(2L, "인기 뉴스", 100);
        Pageable expectedPageable = PageRequest.of(0, 20,
            Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("publishedAt")));
        given(newsRepository.findAll(expectedPageable))
            .willReturn(new PageImpl<>(List.of(news)));

        NewsListResponse response = newsService.getNews(NewsSortType.POPULARITY, 1, 20);

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).viewCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("페이지 크기가 최댓값(100)을 초과하면 BusinessException이 발생한다")
    void 페이지_크기_초과_시_예외_발생() {
        assertThatThrownBy(() -> newsService.getNews(NewsSortType.LATEST, 1, 101))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("뉴스 목록 조회 시 hasNext 플래그가 페이지 정보를 올바르게 반영한다")
    void 페이지네이션_hasNext_정확히_반영() {
        News n1 = buildNews(1L, "뉴스1", 5);
        News n2 = buildNews(2L, "뉴스2", 3);
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Order.desc("publishedAt")));
        Page<News> page = new PageImpl<>(List.of(n1, n2), pageable, 5); // total=5, size=2 → hasNext=true
        given(newsRepository.findAll(pageable)).willReturn(page);

        NewsListResponse response = newsService.getNews(NewsSortType.LATEST, 1, 2);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.totalCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("마지막 페이지에서는 hasNext가 false이다")
    void 마지막_페이지_hasNext_false() {
        News news = buildNews(3L, "마지막 뉴스", 1);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("publishedAt")));
        Page<News> page = new PageImpl<>(List.of(news), pageable, 1); // total=1, size=20 → hasNext=false
        given(newsRepository.findAll(pageable)).willReturn(page);

        NewsListResponse response = newsService.getNews(NewsSortType.LATEST, 1, 20);

        assertThat(response.hasNext()).isFalse();
    }

    // ── 상세 조회 & 조회수 증가 ───────────────────────────────────────────────

    @Test
    @DisplayName("존재하는 뉴스 ID로 상세 조회 시 NewsItemResponse를 반환한다")
    void 존재하는_뉴스_ID_상세_조회_성공() {
        News news = buildNews(10L, "상세 뉴스", 42);
        given(newsRepository.findById(10L)).willReturn(Optional.of(news));
        given(newsViewRepository.save(any(NewsView.class))).willAnswer(inv -> inv.getArgument(0));

        NewsItemResponse response = newsService.getNewsDetail(10L);

        assertThat(response.title()).isEqualTo("상세 뉴스");
        assertThat(response.viewCount()).isEqualTo(42);
    }

    @Test
    @DisplayName("뉴스 상세 조회 시 NewsView가 저장되어 조회 이벤트가 기록된다")
    void 뉴스_상세_조회_시_조회_이벤트_저장() {
        News news = buildNews(10L, "이벤트 뉴스", 5);
        given(newsRepository.findById(10L)).willReturn(Optional.of(news));
        given(newsViewRepository.save(any(NewsView.class))).willAnswer(inv -> inv.getArgument(0));

        newsService.getNewsDetail(10L);

        then(newsViewRepository).should().save(any(NewsView.class));
    }

    @Test
    @DisplayName("존재하지 않는 뉴스 ID 조회 시 BusinessException(NOT_FOUND)이 발생한다")
    void 존재하지_않는_뉴스_ID_조회_시_예외_발생() {
        given(newsRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.getNewsDetail(999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private News buildNews(Long id, String title, int viewCount) {
        News news = mock(News.class);
        given(news.getId()).willReturn(id);
        given(news.getTitle()).willReturn(title);
        given(news.getSummary()).willReturn("요약");
        given(news.getViewCount()).willReturn(viewCount);
        given(news.getPublishedAt()).willReturn(LocalDateTime.now());
        return news;
    }
}

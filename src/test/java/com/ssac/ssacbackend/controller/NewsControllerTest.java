package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.request.NewsSortType;
import com.ssac.ssacbackend.dto.response.NewsListResponse;
import com.ssac.ssacbackend.service.NewsService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("NewsController")
class NewsControllerTest {

    private NewsService newsService;
    private NewsController controller;

    @BeforeEach
    void setUp() {
        newsService = mock(NewsService.class);
        controller = new NewsController(newsService);
    }

    // ── getNews ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getNews - 뉴스 목록 조회")
    class GetNews {

        @Test
        @DisplayName("기본 파라미터로 뉴스 목록 조회 성공 시 200을 반환한다")
        void getNews_기본파라미터() {
            NewsListResponse mockResponse = mock(NewsListResponse.class);
            given(newsService.getNews(NewsSortType.LATEST, 1, 20)).willReturn(mockResponse);

            ResponseEntity<ApiResponse<NewsListResponse>> result =
                controller.getNews("latest", 1, 20);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("popularity 정렬로 뉴스 조회 시 POPULARITY 타입을 서비스에 전달한다")
        void getNews_popularity정렬() {
            NewsListResponse mockResponse = mock(NewsListResponse.class);
            given(newsService.getNews(NewsSortType.POPULARITY, 1, 10)).willReturn(mockResponse);

            controller.getNews("popularity", 1, 10);

            verify(newsService).getNews(NewsSortType.POPULARITY, 1, 10);
        }

        @Test
        @DisplayName("잘못된 정렬값 입력 시 BadRequestException이 발생한다")
        void getNews_잘못된정렬값() {
            assertThatThrownBy(() -> controller.getNews("invalid_sort", 1, 20))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("page가 1 미만이면 BadRequestException이 발생한다")
        void getNews_page_0이하() {
            assertThatThrownBy(() -> controller.getNews("latest", 0, 20))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("LATEST 대소문자 구분없이 허용한다")
        void getNews_LATEST_대소문자() {
            NewsListResponse mockResponse = mock(NewsListResponse.class);
            given(newsService.getNews(NewsSortType.LATEST, 2, 10)).willReturn(mockResponse);

            ResponseEntity<ApiResponse<NewsListResponse>> result =
                controller.getNews("LATEST", 2, 10);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}

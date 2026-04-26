package com.ssac.ssacbackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssac.ssacbackend.domain.news.News;
import com.ssac.ssacbackend.repository.NewsRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 뉴스 목록 API 통합 테스트.
 *
 * <p>루트 패키지에 위치하여 ArchUnit 레이어 규칙 검사 대상에서 제외된다.
 */
@SpringBootTest
@ActiveProfiles("test")
class NewsIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private NewsRepository newsRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        newsRepository.deleteAll();
    }

    // ── 기본 동작 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/news는 인증 없이 200을 응답한다")
    void getNews_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/news"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalCount").value(0))
            .andExpect(jsonPath("$.data.hasNext").value(false))
            .andExpect(jsonPath("$.data.contents").isArray());
    }

    @Test
    @DisplayName("sort 파라미터 없이 요청하면 latest 기본값으로 동작한다")
    void getNews_noSortParam_usesLatestDefault() throws Exception {
        mockMvc.perform(get("/api/news"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("sort=latest 요청 시 publishedAt 내림차순으로 응답한다")
    void getNews_sortLatest_returnsPublishedAtDesc() throws Exception {
        LocalDateTime older = LocalDateTime.now().minusDays(2);
        LocalDateTime newer = LocalDateTime.now().minusDays(1);
        newsRepository.save(News.builder().title("오래된 뉴스").summary("요약1")
            .viewCount(10).publishedAt(older).build());
        newsRepository.save(News.builder().title("최신 뉴스").summary("요약2")
            .viewCount(5).publishedAt(newer).build());

        mockMvc.perform(get("/api/news?sort=latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contents[0].title").value("최신 뉴스"))
            .andExpect(jsonPath("$.data.contents[1].title").value("오래된 뉴스"));
    }

    @Test
    @DisplayName("sort=popularity 요청 시 viewCount 내림차순으로 응답한다")
    void getNews_sortPopularity_returnsViewCountDesc() throws Exception {
        newsRepository.save(News.builder().title("조회수 낮음").summary("요약1")
            .viewCount(10).publishedAt(LocalDateTime.now()).build());
        newsRepository.save(News.builder().title("조회수 높음").summary("요약2")
            .viewCount(500).publishedAt(LocalDateTime.now().minusDays(1)).build());

        mockMvc.perform(get("/api/news?sort=popularity"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contents[0].title").value("조회수 높음"))
            .andExpect(jsonPath("$.data.contents[1].title").value("조회수 낮음"));
    }

    @Test
    @DisplayName("viewCount가 같으면 publishedAt 내림차순으로 정렬된다")
    void getNews_sortPopularity_tieBreakByPublishedAt() throws Exception {
        LocalDateTime older = LocalDateTime.now().minusDays(2);
        LocalDateTime newer = LocalDateTime.now().minusDays(1);
        newsRepository.save(News.builder().title("동점 오래된").summary("요약1")
            .viewCount(100).publishedAt(older).build());
        newsRepository.save(News.builder().title("동점 최신").summary("요약2")
            .viewCount(100).publishedAt(newer).build());

        mockMvc.perform(get("/api/news?sort=popularity"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contents[0].title").value("동점 최신"))
            .andExpect(jsonPath("$.data.contents[1].title").value("동점 오래된"));
    }

    // ── 에러 응답 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("허용되지 않는 sort 값은 400과 INVALID_SORT_PARAMETER 코드를 응답한다")
    void getNews_invalidSort_returns400WithCode() throws Exception {
        mockMvc.perform(get("/api/news?sort=invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_SORT_PARAMETER"))
            .andExpect(jsonPath("$.message").value("허용되지 않는 정렬 기준입니다."));
    }

    @Test
    @DisplayName("size가 최대값(100)을 초과하면 400과 PAGE_SIZE_EXCEEDED 코드를 응답한다")
    void getNews_sizeExceedsMax_returns400WithCode() throws Exception {
        mockMvc.perform(get("/api/news?size=101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("PAGE_SIZE_EXCEEDED"));
    }

    // ── 페이지네이션 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("결과가 없으면 totalCount:0, hasNext:false, contents:[] 를 응답한다")
    void getNews_noData_returnsEmptyResult() throws Exception {
        mockMvc.perform(get("/api/news"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalCount").value(0))
            .andExpect(jsonPath("$.data.hasNext").value(false))
            .andExpect(jsonPath("$.data.contents").isEmpty());
    }

    @Test
    @DisplayName("마지막 페이지이면 hasNext:false를 응답한다")
    void getNews_lastPage_hasNextFalse() throws Exception {
        for (int i = 0; i < 3; i++) {
            newsRepository.save(News.builder().title("뉴스" + i).summary("요약")
                .viewCount(0).publishedAt(LocalDateTime.now()).build());
        }

        mockMvc.perform(get("/api/news?size=3&page=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalCount").value(3))
            .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("다음 페이지가 있으면 hasNext:true를 응답한다")
    void getNews_notLastPage_hasNextTrue() throws Exception {
        for (int i = 0; i < 5; i++) {
            newsRepository.save(News.builder().title("뉴스" + i).summary("요약")
                .viewCount(0).publishedAt(LocalDateTime.now()).build());
        }

        mockMvc.perform(get("/api/news?size=3&page=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalCount").value(5))
            .andExpect(jsonPath("$.data.hasNext").value(true))
            .andExpect(jsonPath("$.data.contents.length()").value(3));
    }

    @Test
    @DisplayName("응답에는 id, title, summary, viewCount, publishedAt 필드가 포함된다")
    void getNews_responseContainsRequiredFields() throws Exception {
        newsRepository.save(News.builder().title("제목").summary("요약")
            .viewCount(42).publishedAt(LocalDateTime.now()).build());

        mockMvc.perform(get("/api/news"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contents[0].id").exists())
            .andExpect(jsonPath("$.data.contents[0].title").value("제목"))
            .andExpect(jsonPath("$.data.contents[0].summary").value("요약"))
            .andExpect(jsonPath("$.data.contents[0].viewCount").value(42))
            .andExpect(jsonPath("$.data.contents[0].publishedAt").exists());
    }
}

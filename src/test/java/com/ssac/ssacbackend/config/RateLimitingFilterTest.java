package com.ssac.ssacbackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitingFilterTest {

    private InMemoryRateLimitStore store;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        store = new InMemoryRateLimitStore();
        filter = new RateLimitingFilter(store);
    }

    // ── shouldNotFilter ────────────────────────────────────────────────────────

    @Test
    @DisplayName("보호 경로에 해당하지 않는 요청은 필터를 통과한다")
    void 비보호_경로_필터_스킵() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/home");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull(); // chain이 호출됨
    }

    // ── 인증 경로 (/api/v1/auth) ──────────────────────────────────────────────

    @Test
    @DisplayName("/api/v1/auth 경로는 분당 10회 초과 시 429를 반환한다")
    void 인증_경로_10회_초과_시_429() throws Exception {
        for (int i = 0; i < 10; i++) {
            callFilter("1.2.3.4", "/api/v1/auth/naver/callback");
        }

        MockHttpServletResponse response = callFilter("1.2.3.4", "/api/v1/auth/naver/callback");

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("/api/v1/auth 경로는 분당 10회 이하는 정상 통과한다")
    void 인증_경로_10회_이하_정상() throws Exception {
        for (int i = 0; i < 9; i++) {
            callFilter("1.2.3.4", "/api/v1/auth/naver/callback");
        }

        MockHttpServletResponse response = callFilter("1.2.3.4", "/api/v1/auth/naver/callback");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    // ── 피드백 경로 (/api/v1/feedback) ────────────────────────────────────────

    @Test
    @DisplayName("/api/v1/feedback 경로는 분당 5회 초과 시 429를 반환한다")
    void 피드백_경로_5회_초과_시_429() throws Exception {
        for (int i = 0; i < 5; i++) {
            callFilter("1.2.3.4", "/api/v1/feedback");
        }

        MockHttpServletResponse response = callFilter("1.2.3.4", "/api/v1/feedback");

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("/api/v1/feedback 경로는 분당 5회 이하는 정상 통과한다")
    void 피드백_경로_5회_이하_정상() throws Exception {
        for (int i = 0; i < 4; i++) {
            callFilter("1.2.3.4", "/api/v1/feedback");
        }

        MockHttpServletResponse response = callFilter("1.2.3.4", "/api/v1/feedback");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    // ── 검색 경로 (/api/v1/search) ────────────────────────────────────────────

    @Test
    @DisplayName("/api/v1/search 경로는 분당 60회 초과 시 429를 반환한다")
    void 검색_경로_60회_초과_시_429() throws Exception {
        for (int i = 0; i < 60; i++) {
            callFilter("1.2.3.4", "/api/v1/search?q=연말정산");
        }

        MockHttpServletResponse response = callFilter("1.2.3.4", "/api/v1/search?q=연말정산");

        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ── 경로별 독립 카운터 ────────────────────────────────────────────────────

    @Test
    @DisplayName("인증 경로와 검색 경로는 카운터가 독립적으로 유지된다")
    void 경로별_독립_카운터() throws Exception {
        // 인증 경로 10회 소진
        for (int i = 0; i < 10; i++) {
            callFilter("1.2.3.4", "/api/v1/auth/naver/callback");
        }

        // 검색 경로는 별도 카운터 → 정상 통과
        MockHttpServletResponse searchResponse = callFilter("1.2.3.4", "/api/v1/search?q=test");
        assertThat(searchResponse.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("다른 IP는 카운터가 독립적으로 유지된다")
    void IP별_독립_카운터() throws Exception {
        for (int i = 0; i < 10; i++) {
            callFilter("1.1.1.1", "/api/v1/auth/status");
        }

        // 다른 IP는 영향 없음
        MockHttpServletResponse response = callFilter("2.2.2.2", "/api/v1/auth/status");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    // ── X-Forwarded-For ───────────────────────────────────────────────────────

    @Test
    @DisplayName("X-Forwarded-For 헤더의 첫 번째 IP를 클라이언트 IP로 사용한다")
    void XForwardedFor_첫번째_IP_사용() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/naver/callback");
            req.addHeader("X-Forwarded-For", "9.9.9.9, 10.0.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/naver/callback");
        req.addHeader("X-Forwarded-For", "9.9.9.9, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(req, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ── 규칙 목록 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("RULES에 인증, 피드백, 검색 경로가 모두 포함된다")
    void RULES_경로_포함_확인() {
        assertThat(RateLimitingFilter.RULES.stream().map(RateLimitRule::pathPrefix))
            .contains("/api/v1/auth", "/api/auth", "/login", "/oauth2",
                "/api/v1/feedback", "/api/v1/search");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private MockHttpServletResponse callFilter(String ip, String uri) throws Exception {
        // URI에서 path 부분만 추출
        String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}

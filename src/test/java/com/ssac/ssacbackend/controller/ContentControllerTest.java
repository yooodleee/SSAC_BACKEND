package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.ContentCompleteResponse;
import com.ssac.ssacbackend.dto.response.ContentDetailResponse;
import com.ssac.ssacbackend.dto.response.ContentListResponse;
import com.ssac.ssacbackend.service.ContentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@DisplayName("ContentController")
class ContentControllerTest {

    private ContentService contentService;
    private ContentController controller;

    @BeforeEach
    void setUp() {
        contentService = mock(ContentService.class);
        controller = new ContentController(contentService);
    }

    // ── getContents ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getContents - 콘텐츠 목록 조회")
    class GetContents {

        @Test
        @DisplayName("필터 없이 목록 조회 성공 시 200을 반환한다")
        void getContents_성공() {
            Authentication auth = mockAuth("user@test.com");
            ContentListResponse mockResponse = new ContentListResponse(5L, List.of());
            given(contentService.getContents(auth, null, null, null)).willReturn(mockResponse);

            ResponseEntity<ApiResponse<ContentListResponse>> result =
                controller.getContents(auth, null, null, null);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("카테고리/난이도/도메인 필터를 서비스에 그대로 전달한다")
        void getContents_필터전달() {
            Authentication auth = mockAuth("user@test.com");
            ContentListResponse mockResponse = new ContentListResponse(1L, List.of());
            given(contentService.getContents(auth, "realestate", "SEED", "finance"))
                .willReturn(mockResponse);

            controller.getContents(auth, "realestate", "SEED", "finance");

            verify(contentService).getContents(auth, "realestate", "SEED", "finance");
        }

        @Test
        @DisplayName("비로그인 사용자(null authentication)도 목록 조회를 요청할 수 있다")
        void getContents_비로그인() {
            ContentListResponse mockResponse = new ContentListResponse(0L, List.of());
            given(contentService.getContents(null, null, null, null)).willReturn(mockResponse);

            ResponseEntity<ApiResponse<ContentListResponse>> result =
                controller.getContents(null, null, null, null);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ── getContent ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getContent - 콘텐츠 상세 조회")
    class GetContent {

        @Test
        @DisplayName("상세 조회 성공 시 200과 ContentDetailResponse를 반환한다")
        void getContent_성공() {
            Authentication auth = mockAuth("user@test.com");
            ContentDetailResponse mockDetail = mock(ContentDetailResponse.class);
            given(contentService.getContent(1L, auth)).willReturn(mockDetail);

            ResponseEntity<ApiResponse<ContentDetailResponse>> result =
                controller.getContent(auth, 1L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData()).isEqualTo(mockDetail);
        }
    }

    // ── complete ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("complete - 콘텐츠 학습 완료")
    class Complete {

        @Test
        @DisplayName("학습 완료 처리 성공 시 200과 ContentCompleteResponse를 반환한다")
        void complete_성공() {
            Authentication auth = mockAuth("user@test.com");
            ContentCompleteResponse mockResponse =
                new ContentCompleteResponse("1", true, false, null, null);
            given(contentService.complete("user@test.com", 1L)).willReturn(mockResponse);

            ResponseEntity<ApiResponse<ContentCompleteResponse>> result =
                controller.complete(auth, 1L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData()).isEqualTo(mockResponse);
        }

        @Test
        @DisplayName("학습 완료 처리 시 올바른 이메일과 contentId를 서비스에 전달한다")
        void complete_서비스_호출검증() {
            Authentication auth = mockAuth("user@test.com");
            ContentCompleteResponse mockResponse =
                new ContentCompleteResponse("42", true, false, null, null);
            given(contentService.complete("user@test.com", 42L)).willReturn(mockResponse);

            controller.complete(auth, 42L);

            verify(contentService).complete("user@test.com", 42L);
        }
    }

    // ── recordView ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordView - 콘텐츠 조회 이력 기록")
    class RecordView {

        @Test
        @DisplayName("이력 기록 성공 시 204를 반환한다")
        void recordView_성공() {
            Authentication auth = mockAuth("user@test.com");

            ResponseEntity<Void> result = controller.recordView(auth, 10L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(contentService).recordView("user@test.com", 10L);
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }
}

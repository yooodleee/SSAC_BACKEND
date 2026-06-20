package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.common.response.PageResponse;
import com.ssac.ssacbackend.dto.request.AttemptSortType;
import com.ssac.ssacbackend.dto.request.QuizSubmitRequest;
import com.ssac.ssacbackend.dto.request.StatPeriod;
import com.ssac.ssacbackend.dto.response.QuizAttemptDetailResponse;
import com.ssac.ssacbackend.dto.response.QuizAttemptSummaryResponse;
import com.ssac.ssacbackend.dto.response.QuizSubmitResponse;
import com.ssac.ssacbackend.dto.response.UserStatsResponse;
import com.ssac.ssacbackend.service.QuizAttemptService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@DisplayName("QuizAttemptController")
class QuizAttemptControllerTest {

    private QuizAttemptService quizAttemptService;
    private QuizAttemptController controller;

    @BeforeEach
    void setUp() {
        quizAttemptService = mock(QuizAttemptService.class);
        controller = new QuizAttemptController(quizAttemptService);
    }

    // ── submitQuiz ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitQuiz - 퀴즈 제출")
    class SubmitQuiz {

        @Test
        @DisplayName("USER 권한으로 제출 시 201과 QuizSubmitResponse를 반환한다")
        void submitQuiz_USER_성공() {
            Authentication auth = mockAuthWithRole("user@test.com", "ROLE_USER");
            QuizSubmitRequest request = new QuizSubmitRequest(
                1L,
                List.of(new QuizSubmitRequest.AnswerItem(1L, "A"))
            );
            QuizSubmitResponse mockResponse = new QuizSubmitResponse(
                1L, 1L, "테스트 퀴즈", 80, 100, 8, 10, 80.0,
                LocalDateTime.now(), false, "SEED", null, null, null
            );
            given(quizAttemptService.submitQuiz(eq("user@test.com"), any())).willReturn(mockResponse);

            ResponseEntity<ApiResponse<?>> result = controller.submitQuiz(auth, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            verify(quizAttemptService).submitQuiz("user@test.com", request);
        }

        @Test
        @DisplayName("GUEST 권한으로 제출 시 201과 QuizAttemptSummaryResponse를 반환한다")
        void submitQuiz_GUEST_성공() {
            Authentication auth = mockAuthWithRole("guest-uuid", "ROLE_GUEST");
            QuizSubmitRequest request = new QuizSubmitRequest(
                1L,
                List.of(new QuizSubmitRequest.AnswerItem(1L, "A"))
            );
            QuizAttemptSummaryResponse mockSummary = new QuizAttemptSummaryResponse(
                1L, 1L, "테스트 퀴즈", 80, 100, 8, 10, 80.0, LocalDateTime.now()
            );
            given(quizAttemptService.submitQuizAsGuest(eq("guest-uuid"), any()))
                .willReturn(mockSummary);

            ResponseEntity<ApiResponse<?>> result = controller.submitQuiz(auth, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            verify(quizAttemptService).submitQuizAsGuest("guest-uuid", request);
        }
    }

    // ── getGuestHistory ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getGuestHistory - 비회원 퀴즈 기록 조회")
    class GetGuestHistory {

        @Test
        @DisplayName("GUEST 기록 조회 성공 시 200과 페이지 결과를 반환한다")
        void getGuestHistory_성공() {
            Authentication auth = mockAuthWithRole("guest-uuid", "ROLE_GUEST");
            Page<QuizAttemptSummaryResponse> page = new PageImpl<>(List.of());
            given(quizAttemptService.getGuestHistory("guest-uuid", 1, 10, AttemptSortType.LATEST))
                .willReturn(page);

            ResponseEntity<ApiResponse<PageResponse<QuizAttemptSummaryResponse>>> result =
                controller.getGuestHistory(auth, 1, 10, AttemptSortType.LATEST);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
        }
    }

    // ── getHistory ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory - 퀴즈 응시 기록 목록 조회")
    class GetHistory {

        @Test
        @DisplayName("회원 기록 목록 조회 성공 시 200을 반환한다")
        void getHistory_성공() {
            Authentication auth = mockAuth("user@test.com");
            Page<QuizAttemptSummaryResponse> page = new PageImpl<>(List.of());
            given(quizAttemptService.getHistory("user@test.com", 1, 10, AttemptSortType.LATEST))
                .willReturn(page);

            ResponseEntity<ApiResponse<PageResponse<QuizAttemptSummaryResponse>>> result =
                controller.getHistory(auth, 1, 10, AttemptSortType.LATEST);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("SCORE 정렬로 기록 조회 시 서비스에 SCORE 정렬 인자를 전달한다")
        void getHistory_SCORE정렬() {
            Authentication auth = mockAuth("user@test.com");
            Page<QuizAttemptSummaryResponse> page = new PageImpl<>(List.of());
            given(quizAttemptService.getHistory("user@test.com", 1, 5, AttemptSortType.SCORE))
                .willReturn(page);

            controller.getHistory(auth, 1, 5, AttemptSortType.SCORE);

            verify(quizAttemptService).getHistory("user@test.com", 1, 5, AttemptSortType.SCORE);
        }
    }

    // ── getDetail ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDetail - 퀴즈 응시 상세 조회")
    class GetDetail {

        @Test
        @DisplayName("상세 조회 성공 시 200과 상세 응답을 반환한다")
        void getDetail_성공() {
            Authentication auth = mockAuth("user@test.com");
            QuizAttemptDetailResponse mockDetail = mock(QuizAttemptDetailResponse.class);
            given(quizAttemptService.getDetail("user@test.com", 5L)).willReturn(mockDetail);

            ResponseEntity<ApiResponse<QuizAttemptDetailResponse>> result =
                controller.getDetail(auth, 5L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData()).isEqualTo(mockDetail);
        }
    }

    // ── getStats ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStats - 사용자 퀴즈 통계 조회")
    class GetStats {

        @Test
        @DisplayName("DAILY 기간 통계 조회 성공 시 200을 반환한다")
        void getStats_DAILY_성공() {
            Authentication auth = mockAuth("user@test.com");
            UserStatsResponse mockStats = UserStatsResponse.empty();
            given(quizAttemptService.getStats("user@test.com", StatPeriod.DAILY))
                .willReturn(mockStats);

            ResponseEntity<ApiResponse<UserStatsResponse>> result =
                controller.getStats(auth, StatPeriod.DAILY);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("WEEKLY 기간으로 통계 조회 시 서비스에 WEEKLY를 전달한다")
        void getStats_WEEKLY() {
            Authentication auth = mockAuth("user@test.com");
            given(quizAttemptService.getStats("user@test.com", StatPeriod.WEEKLY))
                .willReturn(UserStatsResponse.empty());

            controller.getStats(auth, StatPeriod.WEEKLY);

            verify(quizAttemptService).getStats("user@test.com", StatPeriod.WEEKLY);
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }

    @SuppressWarnings("unchecked")
    private Authentication mockAuthWithRole(String name, String role) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        when(auth.getAuthorities()).thenReturn((Collection) authorities);
        return auth;
    }
}

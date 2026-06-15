package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.UserSegmentResponse;
import com.ssac.ssacbackend.service.UserSegmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@DisplayName("UserSegmentController")
class UserSegmentControllerTest {

    private UserSegmentService userSegmentService;
    private UserSegmentController controller;

    @BeforeEach
    void setUp() {
        userSegmentService = mock(UserSegmentService.class);
        controller = new UserSegmentController(userSegmentService);
    }

    // ── getSegment ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSegment - 사용자 세그먼트 조회")
    class GetSegment {

        @Test
        @DisplayName("완료 콘텐츠 5개 미만 사용자는 beginner 세그먼트를 반환한다")
        void getSegment_beginner() {
            Authentication auth = mockAuth("user@test.com");
            given(userSegmentService.getSegment("user@test.com"))
                .willReturn(UserSegmentResponse.beginner());

            ResponseEntity<ApiResponse<UserSegmentResponse>> result =
                controller.getSegment(auth);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData().segment()).isEqualTo("beginner");
        }

        @Test
        @DisplayName("완료 콘텐츠 5개 이상 사용자는 advanced 세그먼트를 반환한다")
        void getSegment_advanced() {
            Authentication auth = mockAuth("power@test.com");
            given(userSegmentService.getSegment("power@test.com"))
                .willReturn(UserSegmentResponse.advanced());

            ResponseEntity<ApiResponse<UserSegmentResponse>> result =
                controller.getSegment(auth);

            assertThat(result.getBody().getData().segment()).isEqualTo("advanced");
        }

        @Test
        @DisplayName("세그먼트 조회 시 인증 사용자 이메일을 서비스에 전달한다")
        void getSegment_이메일전달() {
            Authentication auth = mockAuth("user@test.com");
            given(userSegmentService.getSegment("user@test.com"))
                .willReturn(UserSegmentResponse.beginner());

            controller.getSegment(auth);

            verify(userSegmentService).getSegment("user@test.com");
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }
}

package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.AbTestGroupResponse;
import com.ssac.ssacbackend.service.AbTestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("AbTestController")
class AbTestControllerTest {

    private AbTestService abTestService;
    private AbTestController controller;

    @BeforeEach
    void setUp() {
        abTestService = mock(AbTestService.class);
        controller = new AbTestController(abTestService);
    }

    // ── getMenuGroup ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMenuGroup - 메뉴 A/B 테스트 그룹 조회")
    class GetMenuGroup {

        @Test
        @DisplayName("userId로 조회 시 200과 그룹 응답을 반환한다")
        void getMenuGroup_userId로조회() {
            given(abTestService.assignGroup("user:100")).willReturn("A");

            ResponseEntity<ApiResponse<AbTestGroupResponse>> result =
                controller.getMenuGroup("100", null);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData().group()).isEqualTo("A");
            verify(abTestService).assignGroup("user:100");
        }

        @Test
        @DisplayName("guestId로 조회 시 200과 그룹 응답을 반환한다")
        void getMenuGroup_guestId로조회() {
            given(abTestService.assignGroup("guest:abc-uuid")).willReturn("B");

            ResponseEntity<ApiResponse<AbTestGroupResponse>> result =
                controller.getMenuGroup(null, "abc-uuid");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getData().group()).isEqualTo("B");
            verify(abTestService).assignGroup("guest:abc-uuid");
        }

        @Test
        @DisplayName("userId와 guestId 모두 null이면 BadRequestException이 발생한다")
        void getMenuGroup_식별자없음_예외() {
            assertThatThrownBy(() -> controller.getMenuGroup(null, null))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("userId와 guestId 모두 공백이면 BadRequestException이 발생한다")
        void getMenuGroup_공백식별자_예외() {
            assertThatThrownBy(() -> controller.getMenuGroup("  ", ""))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("userId가 있으면 guestId보다 우선 적용된다")
        void getMenuGroup_userId_우선적용() {
            given(abTestService.assignGroup("user:999")).willReturn("A");

            controller.getMenuGroup("999", "guest-id");

            verify(abTestService).assignGroup("user:999");
        }
    }
}

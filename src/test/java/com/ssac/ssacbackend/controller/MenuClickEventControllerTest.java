package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.dto.request.MenuClickRequest;
import com.ssac.ssacbackend.service.MenuClickEventService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("MenuClickEventController")
class MenuClickEventControllerTest {

    private MenuClickEventService menuClickEventService;
    private MenuClickEventController controller;

    @BeforeEach
    void setUp() {
        menuClickEventService = mock(MenuClickEventService.class);
        controller = new MenuClickEventController(menuClickEventService);
    }

    // ── recordMenuClick ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordMenuClick - 메뉴 클릭 이벤트 수집")
    class RecordMenuClick {

        @Test
        @DisplayName("userId가 있는 이벤트 수신 성공 시 204를 반환한다")
        void recordMenuClick_userId있음() {
            MenuClickRequest request = new MenuClickRequest(
                "CLICK", "menu-home", "홈", "user-1", null,
                LocalDateTime.now(), "/"
            );

            ResponseEntity<Void> result = controller.recordMenuClick(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(menuClickEventService).saveAsync(request);
        }

        @Test
        @DisplayName("guestId가 있는 이벤트 수신 성공 시 204를 반환한다")
        void recordMenuClick_guestId있음() {
            MenuClickRequest request = new MenuClickRequest(
                "CLICK", "menu-quiz", "퀴즈", null, "guest-uuid",
                LocalDateTime.now(), "/quiz"
            );

            ResponseEntity<Void> result = controller.recordMenuClick(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(menuClickEventService).saveAsync(request);
        }

        @Test
        @DisplayName("userId와 guestId 모두 없으면 BadRequestException이 발생한다")
        void recordMenuClick_식별자없음_예외() {
            MenuClickRequest request = new MenuClickRequest(
                "CLICK", "menu-home", "홈", null, null,
                LocalDateTime.now(), "/"
            );

            assertThatThrownBy(() -> controller.recordMenuClick(request))
                .isInstanceOf(BadRequestException.class);
            verify(menuClickEventService, never()).saveAsync(request);
        }

        @Test
        @DisplayName("식별자가 공백이면 BadRequestException이 발생한다")
        void recordMenuClick_식별자공백_예외() {
            MenuClickRequest request = new MenuClickRequest(
                "CLICK", "menu-home", "홈", "  ", "",
                LocalDateTime.now(), "/"
            );

            assertThatThrownBy(() -> controller.recordMenuClick(request))
                .isInstanceOf(BadRequestException.class);
        }
    }
}

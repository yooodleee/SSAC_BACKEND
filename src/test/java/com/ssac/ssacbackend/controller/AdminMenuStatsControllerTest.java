package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.MenuClickStatResponse;
import com.ssac.ssacbackend.service.MenuClickEventService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("AdminMenuStatsController")
class AdminMenuStatsControllerTest {

    private MenuClickEventService menuClickEventService;
    private AdminMenuStatsController controller;

    @BeforeEach
    void setUp() {
        menuClickEventService = mock(MenuClickEventService.class);
        controller = new AdminMenuStatsController(menuClickEventService);
    }

    @Test
    @DisplayName("메뉴 클릭 통계 조회 성공 시 200과 통계 목록을 반환한다")
    void getMenuStats_성공() {
        List<MenuClickStatResponse> mockStats = List.of(
            new MenuClickStatResponse("menu-1", "홈", 100L, 25.5),
            new MenuClickStatResponse("menu-2", "퀴즈", 80L, 20.0)
        );
        given(menuClickEventService.getMenuStats()).willReturn(mockStats);

        ResponseEntity<ApiResponse<List<MenuClickStatResponse>>> result =
            controller.getMenuStats();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getData()).hasSize(2);
        verify(menuClickEventService).getMenuStats();
    }

    @Test
    @DisplayName("통계 데이터가 없으면 빈 목록을 반환한다")
    void getMenuStats_빈결과() {
        given(menuClickEventService.getMenuStats()).willReturn(List.of());

        ResponseEntity<ApiResponse<List<MenuClickStatResponse>>> result =
            controller.getMenuStats();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getData()).isEmpty();
    }
}

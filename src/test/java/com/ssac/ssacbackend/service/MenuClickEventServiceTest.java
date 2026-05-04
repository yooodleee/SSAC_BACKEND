package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ssac.ssacbackend.domain.event.MenuClickEvent;
import com.ssac.ssacbackend.dto.request.MenuClickRequest;
import com.ssac.ssacbackend.dto.response.MenuClickStatResponse;
import com.ssac.ssacbackend.repository.MenuClickEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuClickEventServiceTest {

    @Mock
    private MenuClickEventRepository repository;

    @InjectMocks
    private MenuClickEventService menuClickEventService;

    // ── 메뉴 클릭 이벤트 집계 조회 ────────────────────────────────────────────

    @Test
    @DisplayName("클릭 데이터가 있을 때 CTR이 올바르게 계산된다")
    void 클릭_데이터_존재_시_CTR_계산_정확() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        List<Object[]> rows = List.of(
            new Object[]{"menu-01", "홈", 50L},
            new Object[]{"menu-02", "퀴즈", 30L}
        );
        given(repository.countDistinctUsersSince(any(LocalDateTime.class))).willReturn(100L);
        given(repository.countByMenuIdSince(any(LocalDateTime.class))).willReturn(rows);

        List<MenuClickStatResponse> result = menuClickEventService.getMenuStats();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).menuId()).isEqualTo("menu-01");
        assertThat(result.get(0).clickCount()).isEqualTo(50L);
        // CTR = (50 / 100) * 100 = 50.0
        assertThat(result.get(0).ctr()).isEqualTo(50.0);
        assertThat(result.get(1).ctr()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("전체 사용자 수가 0이면 CTR은 0.0이다")
    void 사용자_수_0일_때_CTR_0() {
        List<Object[]> rows = List.<Object[]>of(new Object[]{"menu-01", "홈", 10L});
        given(repository.countDistinctUsersSince(any(LocalDateTime.class))).willReturn(0L);
        given(repository.countByMenuIdSince(any(LocalDateTime.class))).willReturn(rows);

        List<MenuClickStatResponse> result = menuClickEventService.getMenuStats();

        assertThat(result.get(0).ctr()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("집계 데이터가 없으면 빈 목록을 반환한다")
    void 집계_데이터_없을_때_빈_목록_반환() {
        given(repository.countDistinctUsersSince(any(LocalDateTime.class))).willReturn(0L);
        given(repository.countByMenuIdSince(any(LocalDateTime.class))).willReturn(List.of());

        List<MenuClickStatResponse> result = menuClickEventService.getMenuStats();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("CTR이 소수점 둘째 자리까지 반올림된다")
    void CTR_소수점_둘째_자리_반올림() {
        // clickCount=1, totalUsers=3 → 1/3*100 ≈ 33.33
        List<Object[]> rows = List.<Object[]>of(new Object[]{"menu-01", "홈", 1L});
        given(repository.countDistinctUsersSince(any(LocalDateTime.class))).willReturn(3L);
        given(repository.countByMenuIdSince(any(LocalDateTime.class))).willReturn(rows);

        List<MenuClickStatResponse> result = menuClickEventService.getMenuStats();

        assertThat(result.get(0).ctr()).isEqualTo(33.33);
    }

    // ── 비동기 이벤트 저장 ────────────────────────────────────────────────────

    @Test
    @DisplayName("메뉴 클릭 이벤트 저장 요청 시 repository.save가 호출된다")
    void 메뉴_클릭_이벤트_저장_성공() {
        MenuClickRequest request = new MenuClickRequest(
            "CLICK", "menu-01", "홈", "user-1", null,
            LocalDateTime.now(), "main"
        );
        given(repository.save(any(MenuClickEvent.class))).willAnswer(inv -> inv.getArgument(0));

        menuClickEventService.saveAsync(request);

        then(repository).should().save(any(MenuClickEvent.class));
    }

    @Test
    @DisplayName("저장 중 예외가 발생해도 호출자에게 예외가 전파되지 않는다")
    void 저장_실패_시_예외_전파_안_됨() {
        MenuClickRequest request = new MenuClickRequest(
            "CLICK", "menu-01", "홈", "user-1", null,
            LocalDateTime.now(), "main"
        );
        given(repository.save(any(MenuClickEvent.class))).willThrow(new RuntimeException("DB 오류"));

        // 예외가 전파되지 않으면 테스트 통과
        menuClickEventService.saveAsync(request);
    }
}

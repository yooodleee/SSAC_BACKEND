package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.event.MenuClickEvent;
import com.ssac.ssacbackend.dto.request.MenuClickRequest;
import com.ssac.ssacbackend.dto.response.MenuClickStatResponse;
import com.ssac.ssacbackend.repository.MenuClickEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메뉴 클릭 이벤트 수집 및 집계 서비스.
 *
 * <p>이벤트 저장은 비동기(@Async)로 처리하여 FE 응답에 영향을 주지 않는다.
 * CTR 집계 기간은 7일이며, 분모는 기간 내 고유 사용자(userId 또는 guestId) 수이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuClickEventService {

    private static final int AGGREGATION_DAYS = 7;

    private final MenuClickEventRepository repository;

    /**
     * 메뉴 클릭 이벤트를 비동기로 저장한다.
     * 저장 실패 시 로그만 남기고 호출자에게 예외를 전파하지 않는다.
     */
    @Async
    public void saveAsync(MenuClickRequest request) {
        try {
            MenuClickEvent event = MenuClickEvent.builder()
                .eventType(request.eventType())
                .menuId(request.menuId())
                .menuName(request.menuName())
                .userId(request.userId())
                .guestId(request.guestId())
                .clickedAt(request.clickedAt())
                .pageContext(request.pageContext())
                .build();
            repository.save(event);
        } catch (Exception e) {
            log.error("메뉴 클릭 이벤트 저장 실패: menuId={}, error={}",
                request.menuId(), e.getMessage(), e);
        }
    }

    /**
     * 최근 7일 메뉴별 클릭 수 및 CTR을 집계한다.
     * CTR = 메뉴 클릭 수 / 전체 고유 사용자 수 * 100
     */
    @Transactional(readOnly = true)
    public List<MenuClickStatResponse> getMenuStats() {
        LocalDateTime from = LocalDateTime.now().minusDays(AGGREGATION_DAYS);
        long totalUsers = repository.countDistinctUsersSince(from);
        List<Object[]> rows = repository.countByMenuIdSince(from);

        return rows.stream()
            .map(row -> {
                String menuId = (String) row[0];
                String menuName = (String) row[1];
                long clickCount = (Long) row[2];
                double ctr = totalUsers == 0 ? 0.0
                    : Math.round((double) clickCount / totalUsers * 100.0 * 100.0) / 100.0;
                return new MenuClickStatResponse(menuId, menuName, clickCount, ctr);
            })
            .toList();
    }
}

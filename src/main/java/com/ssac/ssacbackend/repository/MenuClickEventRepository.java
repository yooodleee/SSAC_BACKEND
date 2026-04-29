package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.event.MenuClickEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 메뉴 클릭 이벤트 레포지토리.
 */
public interface MenuClickEventRepository extends JpaRepository<MenuClickEvent, Long> {

    /**
     * 기간 내 메뉴별 클릭 수 집계.
     * 반환: [menuId, menuName, clickCount]
     */
    @Query("""
        SELECT e.menuId, e.menuName, COUNT(e)
        FROM MenuClickEvent e
        WHERE e.clickedAt >= :from
        GROUP BY e.menuId, e.menuName
        ORDER BY COUNT(e) DESC
        """)
    List<Object[]> countByMenuIdSince(@Param("from") LocalDateTime from);

    /**
     * 기간 내 전체 고유 사용자(userId 또는 guestId) 수 집계.
     * 페이지 방문 수의 대리 지표로 사용된다.
     */
    @Query("""
        SELECT COUNT(DISTINCT COALESCE(e.userId, e.guestId))
        FROM MenuClickEvent e
        WHERE e.clickedAt >= :from
        """)
    long countDistinctUsersSince(@Param("from") LocalDateTime from);
}

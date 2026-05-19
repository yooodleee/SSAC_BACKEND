package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.content.ContentViewHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 콘텐츠 조회 이력 레포지토리.
 */
public interface ContentViewHistoryRepository extends JpaRepository<ContentViewHistory, Long> {

    /**
     * 사용자의 콘텐츠 조회 이력을 최신순으로 반환한다.
     *
     * @param userId 사용자 ID
     * @return 조회 이력 목록 (최신순)
     */
    List<ContentViewHistory> findByUserIdOrderByViewedAtDesc(Long userId);

    /**
     * 사용자의 콘텐츠 조회 이력을 최신순으로 반환한다 (JPQL).
     *
     * @param userId 사용자 ID
     * @return 조회 이력 목록 (최신순)
     */
    @Query("SELECT h FROM ContentViewHistory h WHERE h.userId = :userId ORDER BY h.viewedAt DESC")
    List<ContentViewHistory> findViewHistoriesByUserId(@Param("userId") Long userId);
}

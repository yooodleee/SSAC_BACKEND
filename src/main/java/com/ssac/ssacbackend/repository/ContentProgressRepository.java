package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.content.ContentProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 콘텐츠 학습 진행 상황 데이터 접근 인터페이스.
 */
public interface ContentProgressRepository extends JpaRepository<ContentProgress, Long> {

    /**
     * 사용자의 가장 최근에 업데이트된 미완료 진행 항목을 반환한다(이어보기용).
     */
    @Query("""
        SELECT cp FROM ContentProgress cp
        WHERE cp.user.email = :email AND cp.progressRate < 100
        ORDER BY cp.updatedAt DESC
        """)
    Optional<ContentProgress> findLatestInProgressByUserEmail(@Param("email") String email);

    Optional<ContentProgress> findByIdAndUserEmail(Long id, String email);

    /**
     * 사용자의 완료된 콘텐츠 수를 반환한다(세그먼트 산정용).
     */
    long countByUserEmailAndProgressRateGreaterThanEqual(String email, int progressRate);

    void deleteByUserEmail(String email);

    /**
     * 진행률 0% 초과 미완료 콘텐츠 중 가장 최근 항목(이어보기용).
     */
    @Query("""
        SELECT cp FROM ContentProgress cp
        WHERE cp.user.email = :email AND cp.progressRate > 0 AND cp.progressRate < 100
        ORDER BY cp.updatedAt DESC
        """)
    List<ContentProgress> findContinueLearning(@Param("email") String email, Pageable pageable);

    /**
     * 완료된 contentId 목록 반환(추천/todayCard에서 제외 처리용).
     */
    @Query("""
        SELECT cp.contentId FROM ContentProgress cp
        WHERE cp.user.email = :email AND cp.progressRate >= 100 AND cp.contentId IS NOT NULL
        """)
    List<Long> findCompletedContentIdsByUserEmail(@Param("email") String email);

    /**
     * 카테고리별 완료 콘텐츠 수(홈 categories 섹션용).
     */
    @Query("""
        SELECT COUNT(cp) FROM ContentProgress cp
        WHERE cp.user.email = :email AND cp.category = :category AND cp.progressRate >= 100
        """)
    long countCompletedByUserEmailAndCategory(
        @Param("email") String email, @Param("category") String category);
}

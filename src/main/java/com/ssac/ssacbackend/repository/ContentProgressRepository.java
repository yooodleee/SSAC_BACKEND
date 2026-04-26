package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.content.ContentProgress;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

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
}

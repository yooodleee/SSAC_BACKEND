package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.user.LevelHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 레벨 변경 이력 데이터 접근 인터페이스.
 */
public interface LevelHistoryRepository extends JpaRepository<LevelHistory, Long> {
}

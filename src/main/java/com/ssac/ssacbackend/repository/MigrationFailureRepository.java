package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.user.MigrationFailure;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 마이그레이션 실패 기록 데이터 접근 인터페이스.
 */
public interface MigrationFailureRepository extends JpaRepository<MigrationFailure, Long> {
}

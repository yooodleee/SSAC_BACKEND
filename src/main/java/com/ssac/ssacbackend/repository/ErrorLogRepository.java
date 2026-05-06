package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.log.ErrorLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    List<ErrorLog> findByTraceIdOrderByCreatedAtAsc(String traceId);

    @Modifying
    @Query("DELETE FROM ErrorLog e WHERE e.level = :level AND e.createdAt < :cutoff")
    int deleteByLevelAndCreatedAtBefore(
        @Param("level") String level,
        @Param("cutoff") LocalDateTime cutoff
    );
}

package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.repository.ErrorLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 에러 로그 보존 정책 배치.
 *
 * <p>WARN 7일 / ERROR 30일 보존. 자정마다 실행되며 비동기 처리로 서비스 응답에 영향을 주지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorLogBatchService {

    private final ErrorLogRepository errorLogRepository;

    /**
     * 매일 자정(00:00)에 보존 기간 초과 로그를 삭제한다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Async
    @Transactional
    public void purgeExpiredLogs() {
        LocalDateTime warnCutoff = LocalDateTime.now().minusDays(7);
        LocalDateTime errorCutoff = LocalDateTime.now().minusDays(30);

        int warnDeleted = errorLogRepository.deleteByLevelAndCreatedAtBefore("WARN", warnCutoff);
        int errorDeleted = errorLogRepository.deleteByLevelAndCreatedAtBefore("ERROR", errorCutoff);

        log.info("에러 로그 배치 삭제 완료: WARN {}건 (7일 초과), ERROR {}건 (30일 초과)",
            warnDeleted, errorDeleted);
    }
}

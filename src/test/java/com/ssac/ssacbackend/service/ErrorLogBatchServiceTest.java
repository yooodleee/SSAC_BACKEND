package com.ssac.ssacbackend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.repository.ErrorLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ErrorLogBatchService 단위 테스트.
 *
 * <p>보존 정책(WARN 7일 / ERROR 30일) 삭제 호출을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ErrorLogBatchServiceTest {

    @Mock
    private ErrorLogRepository errorLogRepository;

    @InjectMocks
    private ErrorLogBatchService service;

    @Test
    @DisplayName("WARN 레벨 로그를 7일 기준으로 삭제한다")
    void purgeExpiredLogs_WARN_7일_기준_삭제() {
        given(errorLogRepository.deleteByLevelAndCreatedAtBefore(eq("WARN"), any(LocalDateTime.class)))
            .willReturn(3);
        given(errorLogRepository.deleteByLevelAndCreatedAtBefore(eq("ERROR"), any(LocalDateTime.class)))
            .willReturn(0);

        service.purgeExpiredLogs();

        verify(errorLogRepository).deleteByLevelAndCreatedAtBefore(eq("WARN"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("ERROR 레벨 로그를 30일 기준으로 삭제한다")
    void purgeExpiredLogs_ERROR_30일_기준_삭제() {
        given(errorLogRepository.deleteByLevelAndCreatedAtBefore(eq("WARN"), any(LocalDateTime.class)))
            .willReturn(0);
        given(errorLogRepository.deleteByLevelAndCreatedAtBefore(eq("ERROR"), any(LocalDateTime.class)))
            .willReturn(5);

        service.purgeExpiredLogs();

        verify(errorLogRepository).deleteByLevelAndCreatedAtBefore(eq("ERROR"), any(LocalDateTime.class));
    }
}

package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.log.ErrorLog;
import com.ssac.ssacbackend.dto.ErrorLogEntry;
import com.ssac.ssacbackend.repository.ErrorLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ErrorLogServiceTest {

    @Mock
    private ErrorLogRepository errorLogRepository;

    @InjectMocks
    private ErrorLogService errorLogService;

    @Test
    @DisplayName("saveWarn - WARN 레벨 에러 로그를 저장한다")
    void saveWarn_정상() {
        errorLogService.saveWarn("trace-001", "USER-001", "POST", "/api/test", "잘못된 요청", "user-1");

        verify(errorLogRepository).save(any(ErrorLog.class));
    }

    @Test
    @DisplayName("saveWarn - traceId가 null이면 'unknown'으로 저장한다")
    void saveWarn_traceId_null() {
        errorLogService.saveWarn(null, "USER-001", "POST", "/api/test", "잘못된 요청", null);

        verify(errorLogRepository).save(any(ErrorLog.class));
    }

    @Test
    @DisplayName("saveError - ERROR 레벨 에러 로그를 스택 트레이스와 함께 저장한다")
    void saveError_정상() {
        RuntimeException throwable = new RuntimeException("테스트 예외");

        errorLogService.saveError("trace-002", "SERVER-001", "POST", "/api/test",
            "서버 오류", throwable, "user-1");

        verify(errorLogRepository).save(any(ErrorLog.class));
    }

    @Test
    @DisplayName("saveError - throwable이 null이어도 저장에 실패하지 않는다")
    void saveError_throwable_null() {
        errorLogService.saveError("trace-003", "SERVER-001", "POST", "/api/test",
            "서버 오류", null, null);

        verify(errorLogRepository).save(any(ErrorLog.class));
    }

    @Test
    @DisplayName("findByTraceId - traceId로 에러 로그를 조회하여 DTO로 변환한다")
    void findByTraceId_정상() {
        ErrorLog log1 = buildErrorLog("trace-abc", "WARN", "USER-001", "잘못된 요청", null);
        ErrorLog log2 = buildErrorLog("trace-abc", "ERROR", "SERVER-001", "서버 오류", "stack");
        given(errorLogRepository.findByTraceIdOrderByCreatedAtAsc("trace-abc"))
            .willReturn(List.of(log1, log2));

        List<ErrorLogEntry> result = errorLogService.findByTraceId("trace-abc");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).level()).isEqualTo("WARN");
        assertThat(result.get(1).level()).isEqualTo("ERROR");
        assertThat(result.get(1).stackTrace()).isEqualTo("stack");
    }

    @Test
    @DisplayName("findByTraceId - 결과가 없으면 빈 목록을 반환한다")
    void findByTraceId_없음() {
        given(errorLogRepository.findByTraceIdOrderByCreatedAtAsc("unknown"))
            .willReturn(List.of());

        List<ErrorLogEntry> result = errorLogService.findByTraceId("unknown");

        assertThat(result).isEmpty();
    }

    private ErrorLog buildErrorLog(String traceId, String level, String errorCode,
                                   String message, String stackTrace) {
        ErrorLog el = ErrorLog.builder()
            .traceId(traceId)
            .level(level)
            .errorCode(errorCode)
            .method("GET")
            .path("/api/test")
            .message(message)
            .stackTrace(stackTrace)
            .build();
        ReflectionTestUtils.setField(el, "createdAt", LocalDateTime.now());
        return el;
    }
}

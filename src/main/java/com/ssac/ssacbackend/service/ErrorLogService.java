package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.log.ErrorLog;
import com.ssac.ssacbackend.dto.ErrorLogEntry;
import com.ssac.ssacbackend.repository.ErrorLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 에러 로그 저장 서비스.
 *
 * <p>비동기(@Async)로 실행되어 서비스 응답 시간에 영향을 주지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    /**
     * WARN 레벨 에러 로그를 저장한다 (4xx 비즈니스 예외).
     *
     * <p>userId는 호출 스레드에서 MDC가 살아있을 때 미리 읽어 전달한다
     * (@Async 스레드에서는 MDC가 전파되지 않음).
     */
    @Async
    public void saveWarn(String traceId, String errorCode,
                         HttpServletRequest request, String message, String userId) {
        try {
            ErrorLog errorLog = ErrorLog.builder()
                .traceId(traceId != null ? traceId : "unknown")
                .level("WARN")
                .errorCode(errorCode)
                .method(request.getMethod())
                .path(request.getRequestURI())
                .userId(userId)
                .message(message)
                .build();
            errorLogRepository.save(errorLog);
        } catch (Exception ex) {
            log.error("에러 로그 저장 실패 (WARN): traceId={}", traceId, ex);
        }
    }

    /**
     * ERROR 레벨 에러 로그를 스택 트레이스와 함께 저장한다 (500 서버 오류).
     *
     * <p>userId는 호출 스레드에서 MDC가 살아있을 때 미리 읽어 전달한다.
     */
    @Async
    public void saveError(String traceId, String errorCode,
                          HttpServletRequest request, String message,
                          Throwable throwable, String userId) {
        try {
            ErrorLog errorLog = ErrorLog.builder()
                .traceId(traceId != null ? traceId : "unknown")
                .level("ERROR")
                .errorCode(errorCode)
                .method(request.getMethod())
                .path(request.getRequestURI())
                .userId(userId)
                .message(message)
                .stackTrace(toStackTrace(throwable))
                .build();
            errorLogRepository.save(errorLog);
        } catch (Exception ex) {
            log.error("에러 로그 저장 실패 (ERROR): traceId={}", traceId, ex);
        }
    }

    /**
     * traceId로 에러 로그를 조회한다 (관리자 진단 API용).
     *
     * <p>Domain → DTO 변환하여 반환하므로 Controller가 Domain 레이어에 의존하지 않는다.
     */
    public List<ErrorLogEntry> findByTraceId(String traceId) {
        return errorLogRepository.findByTraceIdOrderByCreatedAtAsc(traceId).stream()
            .map(log -> new ErrorLogEntry(
                log.getCreatedAt().toString(),
                log.getLevel(),
                log.getMessage(),
                log.getStackTrace()
            ))
            .toList();
    }

    private String toStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}

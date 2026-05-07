package com.ssac.ssacbackend.common.exception;

import com.ssac.ssacbackend.common.response.ErrorResponse;
import com.ssac.ssacbackend.service.ErrorLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기. 예외 유형별로 명확히 분리된 핸들러와 공통 에러 응답 구조를 반환한다.
 *
 * <p>로깅 정책:
 * <ul>
 *   <li>비즈니스 예외(4xx) → WARN : [ErrorCode] METHOD PATH | traceId=xxx | userId=xxx</li>
 *   <li>예상치 못한 예외(500) → ERROR : 스택 트레이스 포함</li>
 * </ul>
 *
 * <p>변경 기준: docs/conventions.md#예외-처리
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorLogService errorLogService;

    // ── 400 Bad Request ────────────────────────────────────────────────────────

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
        BadRequestException e, HttpServletRequest request) {
        String traceId = warnLog(e, request);
        errorLogService.saveWarn(traceId, e.getErrorCode().getCode(), request, e.getMessage(), MDC.get("userId"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                e.getErrorCode().getCode(),
                e.getMessage(),
                traceId
            ));
    }

    // ── 401 Unauthorized ───────────────────────────────────────────────────────

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
        UnauthorizedException e, HttpServletRequest request) {
        String traceId = warnLog(e, request);
        errorLogService.saveWarn(traceId, e.getErrorCode().getCode(), request, e.getMessage(), MDC.get("userId"));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                e.getErrorCode().getCode(),
                e.getMessage(),
                traceId
            ));
    }

    // ── 403 Forbidden ──────────────────────────────────────────────────────────

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
        ForbiddenException e, HttpServletRequest request) {
        String traceId = warnLog(e, request);
        errorLogService.saveWarn(traceId, e.getErrorCode().getCode(), request, e.getMessage(), MDC.get("userId"));
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                e.getErrorCode().getCode(),
                e.getMessage(),
                traceId
            ));
    }

    // ── 404 Not Found ──────────────────────────────────────────────────────────

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        NotFoundException e, HttpServletRequest request) {
        String traceId = warnLog(e, request);
        errorLogService.saveWarn(traceId, e.getErrorCode().getCode(), request, e.getMessage(), MDC.get("userId"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                e.getErrorCode().getCode(),
                e.getMessage(),
                traceId
            ));
    }

    // ── 409 Conflict ───────────────────────────────────────────────────────────

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
        ConflictException e, HttpServletRequest request) {
        String traceId = warnLog(e, request);
        errorLogService.saveWarn(traceId, e.getErrorCode().getCode(), request, e.getMessage(), MDC.get("userId"));
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                e.getErrorCode().getCode(),
                e.getMessage(),
                traceId
            ));
    }

    // ── Bean Validation (@Valid) ───────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException e, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        String userId = MDC.get("userId");
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors()
            .stream()
            .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
            .collect(Collectors.toList());

        log.warn("[{}] {} {} | traceId={} | userId={}\n-> 유효성 검사 실패: {}",
            ErrorCode.INVALID_INPUT.getCode(),
            request.getMethod(),
            request.getRequestURI(),
            traceId,
            userId,
            fieldErrors);

        errorLogService.saveWarn(traceId, ErrorCode.INVALID_INPUT.getCode(), request,
            "유효성 검사 실패: " + fieldErrors, userId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_INPUT.getCode(),
                ErrorCode.INVALID_INPUT.getMessage(),
                fieldErrors,
                traceId
            ));
    }

    // ── 500 Internal Server Error ──────────────────────────────────────────────
    // 내부 오류 상세 정보(스택 트레이스, 예외 메시지)는 응답에 포함하지 않는다.

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
        Exception e, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        String userId = MDC.get("userId");
        log.error("[{}] {} {} | traceId={} | userId={}\n-> {}",
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            request.getMethod(),
            request.getRequestURI(),
            traceId,
            userId,
            e.getMessage(),
            e);

        errorLogService.saveError(traceId, ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            request, e.getMessage(), e, userId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                traceId
            ));
    }

    // ── 공통 WARN 로깅 ─────────────────────────────────────────────────────────

    private String warnLog(BusinessException e, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        String userId = MDC.get("userId");
        log.warn("[{}] {} {} | traceId={} | userId={}\n-> {}",
            e.getErrorCode().getCode(),
            request.getMethod(),
            request.getRequestURI(),
            traceId,
            userId,
            e.getMessage());
        return traceId;
    }
}

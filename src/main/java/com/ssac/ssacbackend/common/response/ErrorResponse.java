package com.ssac.ssacbackend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * 공통 에러 응답 구조.
 *
 * <pre>
 * {
 *   "status": 404,
 *   "code": "NEWS-001",
 *   "message": "존재하지 않는 뉴스입니다.",
 *   "timestamp": "2024-01-01T00:00:00.000Z"
 * }
 * </pre>
 *
 * <p>Bean Validation 실패 시 {@code errors} 배열이 추가된다.
 * {@code errors}가 null이면 직렬화에서 제외된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    int status,
    String code,
    String message,
    List<FieldError> errors,
    String timestamp
) {

    /**
     * 필드별 유효성 검사 오류.
     *
     * <pre>{"field": "nickname", "message": "닉네임은 2자 이상 10자 이하로 입력해주세요."}</pre>
     */
    public record FieldError(String field, String message) {
    }

    /**
     * 일반 비즈니스 예외 응답 — errors 없음.
     */
    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, null, Instant.now().toString());
    }

    /**
     * Bean Validation 실패 응답 — 필드별 errors 포함.
     */
    public static ErrorResponse of(int status, String code, String message,
        List<FieldError> errors) {
        return new ErrorResponse(status, code, message, errors, Instant.now().toString());
    }
}

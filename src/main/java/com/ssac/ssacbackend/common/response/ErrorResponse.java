package com.ssac.ssacbackend.common.response;

/**
 * 공통 에러 응답 구조.
 *
 * <p>{"status": 401, "code": "UNAUTHORIZED", "message": "인증이 필요합니다."}
 */
public record ErrorResponse(int status, String code, String message) {

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message);
    }
}

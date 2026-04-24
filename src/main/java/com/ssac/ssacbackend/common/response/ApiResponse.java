package com.ssac.ssacbackend.common.response;

import lombok.Getter;

/**
 * 모든 API 응답을 감싸는 공통 래퍼.
 *
 * <p>성공: {"success": true, "data": {...}, "message": null}
 * <p>실패: {"success": false, "data": null, "message": "한국어 오류 메시지"}
 *
 * <p>변경 기준: docs/conventions.md#api-응답-형식
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final boolean loginRequired;

    private ApiResponse(boolean success, T data, String message, boolean loginRequired) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.loginRequired = loginRequired;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, false);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, false);
    }

    /**
     * 로그인이 필요한 기능에 접근했을 때 반환한다. 클라이언트는 loginRequired를 보고 로그인 유도 UI를 표시한다.
     */
    public static <T> ApiResponse<T> loginRequired(String message) {
        return new ApiResponse<>(false, null, message, true);
    }
}

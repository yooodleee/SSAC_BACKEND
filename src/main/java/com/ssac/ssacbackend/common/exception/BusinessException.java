package com.ssac.ssacbackend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 규칙 위반 시 던지는 최상위 예외.
 *
 * <p>메시지는 사용자에게 노출될 수 있으므로 한국어로 작성한다.
 * RuntimeException을 직접 던지지 말고 이 클래스를 사용한다.
 *
 * <p>code를 명시하지 않으면 HttpStatus.name()이 기본 코드로 사용된다.
 *
 * <p>변경 기준: docs/conventions.md#예외-처리
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(String message, HttpStatus status) {
        this(message, status, status.name());
    }

    public BusinessException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(message, HttpStatus.CONFLICT);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(message, HttpStatus.FORBIDDEN);
    }

    /**
     * 허용되지 않는 sort 파라미터 에러.
     */
    public static BusinessException invalidSortParameter() {
        return new BusinessException(
            "허용되지 않는 정렬 기준입니다.",
            HttpStatus.BAD_REQUEST,
            "INVALID_SORT_PARAMETER"
        );
    }

    /**
     * size 파라미터 최댓값 초과 에러.
     */
    public static BusinessException pageSizeExceeded(int maxSize) {
        return new BusinessException(
            "size는 최대 " + maxSize + "까지 허용됩니다.",
            HttpStatus.BAD_REQUEST,
            "PAGE_SIZE_EXCEEDED"
        );
    }
}

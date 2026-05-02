package com.ssac.ssacbackend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 규칙 위반 시 던지는 최상위 추상 예외.
 *
 * <p>직접 인스턴스화하지 않고 세분화된 하위 클래스를 사용한다.
 * <ul>
 *   <li>{@link BadRequestException}    — 400 Bad Request</li>
 *   <li>{@link UnauthorizedException}  — 401 Unauthorized</li>
 *   <li>{@link ForbiddenException}     — 403 Forbidden</li>
 *   <li>{@link NotFoundException}      — 404 Not Found</li>
 *   <li>{@link ConflictException}      — 409 Conflict</li>
 * </ul>
 *
 * <p>변경 기준: docs/conventions.md#예외-처리
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    protected BusinessException(ErrorCode errorCode, HttpStatus httpStatus) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * ErrorCode 기본 메시지 대신 커스텀 메시지를 사용할 때 사용한다.
     *
     * <p>사용 예: 동적 값(ID, 최댓값 등)을 메시지에 포함해야 할 때.
     */
    protected BusinessException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * HttpStatus 반환 — 기존 테스트와의 호환성을 위해 유지.
     */
    public HttpStatus getStatus() {
        return httpStatus;
    }

    /**
     * ErrorCode 문자열 반환 — 기존 테스트와의 호환성을 위해 유지.
     */
    public String getCode() {
        return errorCode.getCode();
    }
}

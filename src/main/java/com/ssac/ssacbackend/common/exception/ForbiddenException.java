package com.ssac.ssacbackend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증은 되었으나 접근 권한이 없을 때 던지는 403 예외.
 *
 * <p>사용 기준:
 * <ul>
 *   <li>타 사용자 리소스 접근 시도</li>
 *   <li>ADMIN 전용 API에 일반 USER가 접근</li>
 *   <li>GUEST가 로그인 전용 기능 접근</li>
 * </ul>
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.FORBIDDEN);
    }
}

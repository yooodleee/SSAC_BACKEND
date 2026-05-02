package com.ssac.ssacbackend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 중복 데이터 저장 시도 시 던지는 409 예외.
 *
 * <p>사용 기준:
 * <ul>
 *   <li>닉네임 중복, 이미 처리된 알림 중복 등</li>
 * </ul>
 */
public class ConflictException extends BusinessException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.CONFLICT);
    }
}

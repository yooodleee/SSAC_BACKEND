package com.ssac.ssacbackend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 존재하지 않는 리소스 조회 시 던지는 404 예외.
 *
 * <p>사용 기준:
 * <ul>
 *   <li>뉴스, 사용자, 알림, 퀴즈 등 존재하지 않는 리소스 조회</li>
 * </ul>
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.NOT_FOUND);
    }
}

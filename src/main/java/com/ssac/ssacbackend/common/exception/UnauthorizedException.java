package com.ssac.ssacbackend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증 실패 시 던지는 401 예외.
 *
 * <p>사용 기준:
 * <ul>
 *   <li>JWT 검증 실패, 토큰 만료, 로그아웃된 토큰 사용</li>
 *   <li>인증 토큰 자체가 없거나 유효하지 않은 경우</li>
 * </ul>
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.UNAUTHORIZED);
    }
}

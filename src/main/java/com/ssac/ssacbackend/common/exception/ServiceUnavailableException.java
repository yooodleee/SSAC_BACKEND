package com.ssac.ssacbackend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 외부 의존 서비스(Redis 등)가 일시적으로 불가할 때 던지는 503 예외.
 *
 * <p>사용 기준:
 * <ul>
 *   <li>Redis 연결 장애로 인증 상태 저장/조회 불가</li>
 *   <li>재시도로 해결 가능한 일시적 인프라 장애</li>
 * </ul>
 */
public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE);
    }
}

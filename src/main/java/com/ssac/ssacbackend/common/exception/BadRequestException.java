package com.ssac.ssacbackend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 입력값 유효성 검사 실패 시 던지는 400 예외.
 *
 * <p>사용 기준:
 * <ul>
 *   <li>닉네임 형식 오류, 잘못된 정렬 파라미터, 페이지 범위 초과 등</li>
 *   <li>비즈니스 규칙 위반(예: GUEST 역할 부여 시도)</li>
 *   <li>OAuth 인증 실패(잘못된 state, 외부 API 오류)</li>
 * </ul>
 */
public class BadRequestException extends BusinessException {

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.BAD_REQUEST);
    }

    /**
     * 동적 값(ID, 최댓값 등)을 메시지에 포함해야 할 때 사용한다.
     *
     * <p>응답의 {@code code} 필드는 errorCode.getCode()를 사용하고,
     * {@code message} 필드는 전달된 message를 사용한다.
     */
    public BadRequestException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.BAD_REQUEST, message);
    }
}

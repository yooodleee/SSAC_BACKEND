package com.ssac.ssacbackend.dto.request;

/**
 * 관리자 코드 로그인 요청 DTO.
 */
public record AdminLoginRequest(
    String adminCode
) {
}

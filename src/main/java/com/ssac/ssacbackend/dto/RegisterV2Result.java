package com.ssac.ssacbackend.dto;

import com.ssac.ssacbackend.dto.response.RegisterV2Response;

/**
 * 신규 회원 가입 서비스 내부 반환값.
 *
 * <p>refreshToken은 HttpOnly Cookie로만 전달되므로 응답 body에 포함하지 않는다.
 * Controller가 refreshToken을 꺼내 쿠키로 설정한 뒤 response만 직렬화한다.
 */
public record RegisterV2Result(
    String refreshToken,
    RegisterV2Response response
) {}

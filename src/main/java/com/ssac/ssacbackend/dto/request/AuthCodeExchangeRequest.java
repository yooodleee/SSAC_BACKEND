package com.ssac.ssacbackend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 일회용 인가 코드를 JWT 토큰으로 교환하는 요청 DTO.
 */
public record AuthCodeExchangeRequest(

    @NotBlank(message = "authCode는 필수입니다.")
    @Schema(description = "BE OAuth 콜백이 발급한 일회용 인가 코드", example = "550e8400-e29b-41d4-a716-446655440000")
    String authCode

) {
}

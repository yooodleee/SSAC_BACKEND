package com.ssac.ssacbackend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 닉네임 설정 및 회원 가입 완료 요청 DTO.
 */
public record RegisterRequest(

    @NotBlank
    String tempToken,

    @NotBlank
    String nickname,

    String guestId
) {}

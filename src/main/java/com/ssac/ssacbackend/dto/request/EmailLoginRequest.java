package com.ssac.ssacbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일+비밀번호 로그인 요청 DTO (POST /api/v1/auth/login/email).
 */
public record EmailLoginRequest(

    @NotBlank(message = "이메일 형식이 올바르지 않습니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotBlank
    String password

) {}

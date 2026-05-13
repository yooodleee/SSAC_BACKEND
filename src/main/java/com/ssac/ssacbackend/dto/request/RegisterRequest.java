package com.ssac.ssacbackend.dto.request;

import com.ssac.ssacbackend.domain.user.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 닉네임 설정 및 회원 가입 완료 요청 DTO.
 */
public record RegisterRequest(

    @NotBlank
    String tempToken,

    @NotBlank
    String nickname,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "사용자 유형 (HIGH_SCHOOL: 고3 학생 | EARLY_CAREER: 사회초년생)",
        allowableValues = {"HIGH_SCHOOL", "EARLY_CAREER"}
    )
    UserType userType,

    String guestId
) {}

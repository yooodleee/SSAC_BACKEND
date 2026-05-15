package com.ssac.ssacbackend.dto.request;

import com.ssac.ssacbackend.domain.user.UserType;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 유형 수정 요청 DTO.
 */
public record UpdateUserTypeRequest(

    @NotNull(message = "사용자 유형은 필수입니다.")
    UserType userType

) {
}

package com.ssac.ssacbackend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 이어보기 진행 상황 갱신 요청 DTO.
 */
public record UpdateProgressRequest(
    @NotBlank(message = "lastPosition은 필수입니다.")
    String lastPosition,

    @NotNull(message = "progressRate는 필수입니다.")
    @Min(value = 0, message = "progressRate는 0 이상이어야 합니다.")
    @Max(value = 100, message = "progressRate는 100 이하이어야 합니다.")
    Integer progressRate
) {}

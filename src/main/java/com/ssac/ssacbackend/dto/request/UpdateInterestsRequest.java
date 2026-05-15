package com.ssac.ssacbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 관심 도메인 수정 요청 DTO.
 */
public record UpdateInterestsRequest(

    @NotNull(message = "관심 도메인 목록은 필수입니다.")
    @Size(min = 1, max = 3, message = "관심 도메인은 1개 이상 3개 이하로 선택해주세요.")
    List<String> domainIds

) {
}

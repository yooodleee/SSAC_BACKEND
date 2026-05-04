package com.ssac.ssacbackend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 약관 동의 저장 요청 DTO.
 */
public record TermsRequest(

    @NotBlank
    String tempToken,

    @NotNull @Valid
    Agreements agreements
) {
    public record Agreements(
        @NotNull Boolean serviceTerm,
        @NotNull Boolean privacyTerm,
        @NotNull Boolean ageVerification,
        Boolean marketingTerm
    ) {}
}

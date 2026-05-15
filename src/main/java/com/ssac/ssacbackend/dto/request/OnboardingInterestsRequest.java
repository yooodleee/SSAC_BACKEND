package com.ssac.ssacbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 관심 도메인 저장 요청 DTO.
 */
public record OnboardingInterestsRequest(
    @NotNull List<String> domainIds
) {}

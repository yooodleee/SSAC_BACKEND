package com.ssac.ssacbackend.dto.response;

/**
 * 개인정보 수정 결과 응답 DTO.
 */
public record UpdateProfileResponse(
    String name,
    String birthDate,
    String phone,
    String gender,
    String email
) {}

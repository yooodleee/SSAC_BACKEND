package com.ssac.ssacbackend.dto.request;

/**
 * 개인정보 수정 요청 DTO.
 *
 * <p>null 필드는 변경하지 않는다.
 */
public record UpdateProfileRequest(
    String name,
    String birthDate,
    String phone,
    String gender,
    String email
) {}

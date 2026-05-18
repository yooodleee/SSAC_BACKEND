package com.ssac.ssacbackend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 신규 회원 가입 요청 DTO (POST /api/v1/auth/register).
 *
 * <p>소셜 인증 완료 후 사용자가 입력한 개인 정보와 약관 동의 정보를 담는다.
 * tempToken은 소셜 인증 시 발급된 임시 토큰으로 10분간 유효하다.
 */
public record RegisterV2Request(

    @NotBlank
    String tempToken,

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(min = 1, max = 20, message = "이름을 입력해주세요.")
    String name,

    @NotBlank(message = "생년월일 형식이 올바르지 않습니다.")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "생년월일 형식이 올바르지 않습니다.")
    String birthDate,

    @NotBlank(message = "휴대폰 번호 형식이 올바르지 않습니다.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
    String phone,

    /** 성별. null 허용 (선택 입력). MALE / FEMALE / REFUSED */
    String gender,

    @NotBlank(message = "이메일 형식이 올바르지 않습니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotNull @Valid
    Agreements agreements,

    /** 게스트 데이터 병합용 guestId. 비회원 사용 이력이 없으면 null. */
    String guestId

) {
    public record Agreements(
        @NotNull Boolean serviceTerm,
        @NotNull Boolean privacyTerm,
        @NotNull Boolean ageVerification,
        Boolean marketingTerm
    ) {}
}

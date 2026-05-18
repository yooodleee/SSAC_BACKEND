package com.ssac.ssacbackend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 이메일+비밀번호 직접 회원가입 요청 DTO (POST /api/v1/auth/register/email).
 *
 * <p>소셜 인증 없이 이메일·비밀번호·개인 정보를 직접 입력하여 가입한다.
 * 비밀번호 복잡도 검증(8~20자, 영문+숫자)은 Service 레이어에서 수행한다.
 */
public record EmailRegisterRequest(

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

    /** 비밀번호. 8~20자, 영문·숫자 포함 필수. 복잡도 검증은 Service에서 수행한다. */
    @NotBlank
    String password,

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

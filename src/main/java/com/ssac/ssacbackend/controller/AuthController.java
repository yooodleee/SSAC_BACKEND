package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.dto.request.RegisterRequest;
import com.ssac.ssacbackend.dto.request.TermsRequest;
import com.ssac.ssacbackend.dto.response.NicknameCheckResponse;
import com.ssac.ssacbackend.dto.response.RegisterResponse;
import com.ssac.ssacbackend.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 소셜 로그인 기반 회원 가입 API.
 *
 * <p>약관 동의 저장 → 닉네임 설정 → 가입 완료의 3단계 플로우를 제공한다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "회원 가입", description = "소셜 로그인 신규 회원 가입 API")
public class AuthController {

    private final RegistrationService registrationService;

    @PostMapping("/terms")
    @Operation(
        summary = "약관 동의 저장",
        description = "tempToken으로 식별된 임시 등록 항목에 약관 동의 정보를 저장한다. "
            + "필수 약관(serviceTerm, privacyTerm, ageVerification) 미동의 시 400을 반환한다. "
            + "tempToken 만료 시 401을 반환한다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "약관 동의 저장 성공"),
        @ApiResponse(responseCode = "400", description = "TERMS-001: 필수 약관 미동의"),
        @ApiResponse(responseCode = "401", description = "TERMS-002: 회원가입 세션 만료")
    })
    public ResponseEntity<Void> saveTerms(@RequestBody @Valid TermsRequest request) {
        registrationService.saveTerms(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    @Operation(
        summary = "닉네임 설정 및 회원 가입 완료",
        description = "tempToken, 닉네임, userType(HIGH_SCHOOL|EARLY_CAREER), 선택적 guestId를 받아 회원 가입을 완료한다. "
            + "닉네임 규칙: 한글·영문·숫자만 허용, 2~10자, 특수문자 불허. "
            + "성공 시 Access/Refresh Token과 사용자 정보(userType, level, onboardingCompleted 포함)를 반환한다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "회원가입 성공"),
        @ApiResponse(responseCode = "400", description =
            "USER-TYPE-001: userType 누락 | "
            + "USER-TYPE-002: 유효하지 않은 userType 값 | "
            + "USER-003: 닉네임 형식 오류 | "
            + "TERMS-001: 약관 동의 미완료"),
        @ApiResponse(responseCode = "401", description = "TERMS-002: 회원가입 세션 만료"),
        @ApiResponse(responseCode = "409", description = "USER-002: 이미 사용 중인 닉네임")
    })
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = registrationService.register(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/nickname/check")
    @Operation(
        summary = "닉네임 중복 확인",
        description = "닉네임 형식 검증 및 중복 여부를 확인한다. "
            + "형식이 유효하지 않으면 400, 사용 가능하면 isAvailable: true를 반환한다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "확인 성공 (isAvailable: true/false)"),
        @ApiResponse(responseCode = "400", description = "USER-003: 닉네임 형식 오류")
    })
    public ResponseEntity<NicknameCheckResponse> checkNickname(
        @Parameter(description = "확인할 닉네임")
        @RequestParam @NotBlank String nickname
    ) {
        NicknameCheckResponse response = registrationService.checkNickname(nickname);
        return ResponseEntity.ok(response);
    }
}

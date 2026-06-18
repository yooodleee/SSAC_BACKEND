package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.common.util.CookieUtils;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.RegisterV2Result;
import com.ssac.ssacbackend.dto.request.EmailLoginRequest;
import com.ssac.ssacbackend.dto.request.EmailRegisterRequest;
import com.ssac.ssacbackend.dto.request.RegisterV2Request;
import com.ssac.ssacbackend.dto.response.EmailCheckResponse;
import com.ssac.ssacbackend.service.EmailAuthService;
import com.ssac.ssacbackend.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
 * 신규 회원 가입 API (v1).
 *
 * <p>소셜 인증 후 사용자 정보 입력 및 약관 동의를 한 번에 처리한다.
 * Refresh Token은 응답 body에 포함하지 않고 HttpOnly Cookie로 전달한다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "회원 가입 v1", description = "신규 회원 가입 및 이메일 중복 확인 API")
public class AuthV1Controller {

    private final RegistrationService registrationService;
    private final EmailAuthService emailAuthService;
    private final CookieProperties cookieProperties;

    @PostMapping("/register")
    @Operation(
        summary = "신규 회원 가입 완료",
        description = """
            [호출 화면] 회원 가입 정보 입력 화면.
            [권한 조건] 공개 (tempToken으로 신원 확인).
            [특이 동작] Refresh Token은 응답 body에 포함하지 않고 HttpOnly Cookie(Path=/api/v1/auth)로 전달한다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "NAME-001: 이름 미입력 | BIRTH-001: 생일 형식 오류 | BIRTH-002: 만 14세 미만 | "
                + "PHONE-001: 휴대폰 형식 오류 | GENDER-001: 성별 값 오류 | "
                + "EMAIL-001: 이메일 형식 오류 | TERMS-001: 필수 약관 미동의"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "TERMS-002: 회원가입 세션 만료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
            description = "EMAIL-002: 이메일 중복")
    })
    public ResponseEntity<ApiResponse<com.ssac.ssacbackend.dto.response.RegisterV2Response>> register(
        @RequestBody @Valid RegisterV2Request request,
        HttpServletResponse httpResponse
    ) {
        RegisterV2Result result = registrationService.registerV2(request);
        CookieUtils.addRefreshTokenCookie(httpResponse, result.refreshToken(), cookieProperties);
        return ResponseEntity.ok(ApiResponse.success(result.response()));
    }

    @PostMapping("/register/email")
    @Operation(
        summary = "이메일+비밀번호 직접 회원가입",
        description = """
            [호출 화면] 회원 가입 정보 입력 화면 (소셜 로그인 미사용 경로).
            [권한 조건] 공개.
            [특이 동작] Refresh Token은 응답 body에 포함하지 않고 HttpOnly Cookie(Path=/api/v1/auth)로 전달한다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "NAME-001: 이름 미입력 | BIRTH-001: 생일 형식 오류 | BIRTH-002: 만 14세 미만 | "
                + "PHONE-001: 휴대폰 형식 오류 | GENDER-001: 성별 값 오류 | "
                + "EMAIL-001: 이메일 형식 오류 | PASSWORD-001: 비밀번호 형식 오류 | TERMS-001: 필수 약관 미동의"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
            description = "EMAIL-002: 이메일 중복 | PHONE-002: 휴대폰 번호 중복")
    })
    public ResponseEntity<ApiResponse<com.ssac.ssacbackend.dto.response.RegisterV2Response>> registerWithEmail(
        @RequestBody @Valid EmailRegisterRequest request,
        HttpServletResponse httpResponse
    ) {
        RegisterV2Result result = registrationService.registerWithEmail(request);
        CookieUtils.addRefreshTokenCookie(httpResponse, result.refreshToken(), cookieProperties);
        return ResponseEntity.ok(ApiResponse.success(result.response()));
    }

    @PostMapping("/login/email")
    @Operation(
        summary = "이메일+비밀번호 로그인",
        description = """
            [호출 화면] 로그인 화면 (소셜 로그인 미사용 경로).
            [권한 조건] 공개.
            [특이 동작] Refresh Token은 응답 body에 포함하지 않고 HttpOnly Cookie(Path=/api/v1/auth)로 전달한다.
            이메일 미존재와 비밀번호 불일치를 동일한 AUTH-011로 응답한다 (보안).
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "EMAIL-001: 이메일 형식 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "AUTH-011: 이메일 또는 비밀번호 불일치")
    })
    public ResponseEntity<ApiResponse<com.ssac.ssacbackend.dto.response.RegisterV2Response>> loginWithEmail(
        @RequestBody @Valid EmailLoginRequest request,
        HttpServletResponse httpResponse
    ) {
        RegisterV2Result result = emailAuthService.loginWithEmail(request);
        CookieUtils.addRefreshTokenCookie(httpResponse, result.refreshToken(), cookieProperties);
        return ResponseEntity.ok(ApiResponse.success(result.response()));
    }

    @GetMapping("/email/check")
    @Operation(
        summary = "이메일 중복 확인",
        description = """
            [호출 화면] 회원 가입 정보 입력 화면.
            [권한 조건] 공개.
            [특이 동작] 이메일 형식 오류 시 400, 사용 가능하면 isAvailable: true를 반환한다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "확인 성공 (isAvailable: true/false)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "EMAIL-001: 이메일 형식 오류")
    })
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(
        @Parameter(description = "확인할 이메일 주소")
        @RequestParam @NotBlank @Email String email
    ) {
        EmailCheckResponse result = registrationService.checkEmail(email);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.common.util.CookieUtils;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.request.UpdateNicknameRequest;
import com.ssac.ssacbackend.dto.response.ProfileResponse;
import com.ssac.ssacbackend.service.ProfileService;
import com.ssac.ssacbackend.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로필 조회 및 닉네임 수정 엔드포인트.
 *
 * <p>모든 요청은 유효한 JWT Bearer 토큰이 있어야 한다.
 */
@Slf4j
@Tag(name = "Profile", description = "프로필 조회 및 수정 API")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final TokenService tokenService;
    private final CookieProperties cookieProperties;

    @Operation(
        summary = "내 프로필 조회",
        description = """
            [호출 화면] 마이페이지 진입 시 호출.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN). 비회원(GUEST) 접근 불가.
            [특이 동작] 토큰의 이메일로 사용자를 식별하며 타인 프로필 조회 불가.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "프로필 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "loginRequired: true (회원 로그인 필요)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
        Authentication authentication) {
        log.debug("프로필 조회 요청: email={}", authentication.getName());
        ProfileResponse profile = profileService.getProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @Operation(
        summary = "닉네임 수정",
        description = """
            [호출 화면] 마이페이지 > 프로필 수정 섹션에서 닉네임 수정 시 호출.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] 닉네임 중복 시 409 반환. 닉네임 정책: 2~20자, 특수문자 제한.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "닉네임 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "닉네임 유효성 검사 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "loginRequired: true (회원 로그인 필요)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "이미 사용 중인 닉네임")
    })
    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateNickname(
        Authentication authentication,
        @RequestBody @Valid UpdateNicknameRequest request) {
        log.debug("닉네임 수정 요청: email={}", authentication.getName());
        ProfileResponse profile =
            profileService.updateNickname(authentication.getName(), request.nickname());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @Operation(
        summary = "전체 디바이스 로그아웃",
        description = """
            [호출 화면] 마이페이지 > 설정 > 보안 관리 > 모든 기기에서 로그아웃 클릭 시.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] 해당 계정으로 발급된 모든 Refresh Token을 무효화하고 관련 쿠키를 모두 삭제한다.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "전체 디바이스 로그아웃 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "loginRequired: true (회원 로그인 필요)")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
        Authentication authentication,
        HttpServletResponse response) {
        log.debug("전체 디바이스 로그아웃 요청: email={}", authentication.getName());
        tokenService.logoutAll(authentication.getName());
        CookieUtils.clearAccessTokenCookie(response, cookieProperties);
        CookieUtils.clearRefreshTokenCookie(response, cookieProperties);
        CookieUtils.clearGuestIdCookie(response, cookieProperties);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

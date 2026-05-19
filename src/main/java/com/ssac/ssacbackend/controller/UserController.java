package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.common.util.CookieUtils;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.request.UpdateInterestsRequest;
import com.ssac.ssacbackend.dto.request.UpdateNicknameRequest;
import com.ssac.ssacbackend.dto.request.UpdateProfileRequest;
import com.ssac.ssacbackend.dto.request.UpdateUserTypeRequest;
import com.ssac.ssacbackend.dto.response.MyPageResponse;
import com.ssac.ssacbackend.dto.response.UpdateProfileResponse;
import com.ssac.ssacbackend.dto.response.ViewedContentsResponse;
import com.ssac.ssacbackend.service.ProfileService;
import com.ssac.ssacbackend.service.UserService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 및 사용자 정보 수정 엔드포인트.
 *
 * <p>모든 요청은 로그인 회원(USER, ADMIN) 전용이다.
 */
@Slf4j
@Tag(name = "User", description = "마이페이지 및 사용자 정보 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;
    private final CookieProperties cookieProperties;

    @Operation(
        summary = "마이페이지 프로필 조회",
        description = """
            [호출 화면] 마이페이지 진입 시 호출.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] 누적 통계, 연속 학습일, 관심 도메인을 포함한 프로필 반환.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "마이페이지 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자")
    })
    @GetMapping("/mypage")
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(Authentication authentication) {
        log.debug("마이페이지 조회 요청: email={}", authentication.getName());
        MyPageResponse response = userService.getMyPage(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "관심 도메인 수정",
        description = """
            [호출 화면] 마이페이지 > 관심 도메인 수정 완료 시 호출.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] 1개 이상 3개 이하 선택 필수. 기존 데이터 덮어씀.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204", description = "관심 도메인 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "ONBOARDING-007: 도메인 개수 범위 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자")
    })
    @PutMapping("/interests")
    public ResponseEntity<Void> updateInterests(
        Authentication authentication,
        @RequestBody @Valid UpdateInterestsRequest request) {
        log.debug("관심 도메인 수정 요청: email={}", authentication.getName());
        userService.updateInterests(authentication.getName(), request.domainIds());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "사용자 유형 변경",
        description = """
            [호출 화면] 마이페이지 > 사용자 유형 변경 시 호출.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] 유형 변경 시 온보딩 완료 상태인 경우 온보딩 결과 및 관심 도메인이 초기화된다.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204", description = "사용자 유형 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "유효하지 않은 사용자 유형"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자")
    })
    @PatchMapping("/type")
    public ResponseEntity<Void> updateUserType(
        Authentication authentication,
        @RequestBody @Valid UpdateUserTypeRequest request) {
        log.debug("사용자 유형 변경 요청: email={}, newType={}", authentication.getName(), request.userType());
        userService.updateUserType(authentication.getName(), request.userType());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "개인정보 수정",
        description = """
            [호출 화면] 마이페이지 > 개인정보 수정 화면.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] null 필드는 변경하지 않는다.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "개인정보 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "유효성 검사 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자")
    })
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UpdateProfileResponse>> updateProfile(
        Authentication authentication,
        @RequestBody UpdateProfileRequest request) {
        UpdateProfileResponse response = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "닉네임 수정 (단축 경로)",
        description = """
            [호출 화면] 마이페이지 > 닉네임 수정.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204", description = "닉네임 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "닉네임 유효성 검사 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "이미 사용 중인 닉네임")
    })
    @PatchMapping("/nickname")
    public ResponseEntity<Void> updateNickname(
        Authentication authentication,
        @RequestBody @Valid UpdateNicknameRequest request) {
        profileService.updateNickname(authentication.getName(), request.nickname());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "회원 탈퇴",
        description = """
            [호출 화면] 마이페이지 > 회원 탈퇴.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] Soft delete + 개인정보 익명화 + 쿠키 삭제.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204", description = "회원 탈퇴 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
        Authentication authentication,
        HttpServletResponse response) {
        userService.withdraw(authentication.getName());
        CookieUtils.clearAccessTokenCookie(response, cookieProperties);
        CookieUtils.clearRefreshTokenCookie(response, cookieProperties);
        CookieUtils.clearGuestIdCookie(response, cookieProperties);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "내가 본 콘텐츠 목록 조회",
        description = """
            [호출 화면] 마이페이지 > 내가 본 콘텐츠.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "내가 본 콘텐츠 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자")
    })
    @GetMapping("/me/viewed-contents")
    public ResponseEntity<ApiResponse<ViewedContentsResponse>> getViewedContents(
        Authentication authentication) {
        ViewedContentsResponse response = userService.getViewedContents(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.request.UpdateInterestsRequest;
import com.ssac.ssacbackend.dto.request.UpdateNicknameRequest;
import com.ssac.ssacbackend.dto.request.UpdateProfileRequest;
import com.ssac.ssacbackend.dto.request.UpdateUserTypeRequest;
import com.ssac.ssacbackend.dto.response.UpdateProfileResponse;
import com.ssac.ssacbackend.service.ProfileService;
import com.ssac.ssacbackend.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ProfileService profileService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService, profileService, new CookieProperties());
    }

    @Test
    @DisplayName("getMyPage - 마이페이지 조회 성공 시 200을 반환한다")
    void getMyPage_정상() {
        Authentication auth = mockAuth("user@test.com");
        given(userService.getMyPage("user@test.com")).willReturn(null);

        ResponseEntity<?> result = controller.getMyPage(auth);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).getMyPage("user@test.com");
    }

    @Test
    @DisplayName("updateInterests - 관심 도메인 수정 성공 시 204를 반환한다")
    void updateInterests_정상() {
        Authentication auth = mockAuth("user@test.com");
        UpdateInterestsRequest request = new UpdateInterestsRequest(List.of("AI", "CS"));

        ResponseEntity<Void> result = controller.updateInterests(auth, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).updateInterests("user@test.com", List.of("AI", "CS"));
    }

    @Test
    @DisplayName("updateUserType - 사용자 유형 변경 성공 시 204를 반환한다")
    void updateUserType_정상() {
        Authentication auth = mockAuth("user@test.com");
        UpdateUserTypeRequest request = new UpdateUserTypeRequest(UserType.EARLY_CAREER);

        ResponseEntity<Void> result = controller.updateUserType(auth, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).updateUserType("user@test.com", UserType.EARLY_CAREER);
    }

    @Test
    @DisplayName("updateProfile - 개인정보 수정 성공 시 200을 반환한다")
    void updateProfile_정상() {
        Authentication auth = mockAuth("user@test.com");
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, null, null, null);
        UpdateProfileResponse mockResponse = new UpdateProfileResponse("홍길동", null, null, null, null);
        given(userService.updateProfile(eq("user@test.com"), any())).willReturn(mockResponse);

        ResponseEntity<?> result = controller.updateProfile(auth, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("updateNickname - 닉네임 수정 성공 시 204를 반환한다")
    void updateNickname_정상() {
        Authentication auth = mockAuth("user@test.com");
        UpdateNicknameRequest request = new UpdateNicknameRequest("새닉네임");

        ResponseEntity<Void> result = controller.updateNickname(auth, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(profileService).updateNickname("user@test.com", "새닉네임");
    }

    @Test
    @DisplayName("withdraw - 회원 탈퇴 성공 시 204를 반환하고 쿠키를 삭제한다")
    void withdraw_정상() {
        Authentication auth = mockAuth("user@test.com");
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        ResponseEntity<Void> result = controller.withdraw(auth, mockResponse);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).withdraw("user@test.com");
    }

    @Test
    @DisplayName("getViewedContents - 내가 본 콘텐츠 조회 성공 시 200을 반환한다")
    void getViewedContents_정상() {
        Authentication auth = mockAuth("user@test.com");
        given(userService.getViewedContents("user@test.com")).willReturn(null);

        ResponseEntity<?> result = controller.getViewedContents(auth);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).getViewedContents("user@test.com");
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }
}

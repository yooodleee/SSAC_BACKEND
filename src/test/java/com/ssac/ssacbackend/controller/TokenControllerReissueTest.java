package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.response.ReissueResponse;
import com.ssac.ssacbackend.service.JwtService;
import com.ssac.ssacbackend.service.ReissueResult;
import com.ssac.ssacbackend.service.TokenService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

class TokenControllerReissueTest {

    private TokenService tokenService;
    private TokenController controller;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        JwtService jwtService = mock(JwtService.class);

        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");

        controller = new TokenController(tokenService, jwtService, cookieProperties);
    }

    @Test
    @DisplayName("유효한 refreshToken 쿠키가 있으면 200과 함께 사용자 컨텍스트를 반환한다")
    void reissue_유효한_토큰_성공() {
        User user = mockUser(1L, "test@test.com", "닉네임123", UserType.HIGH_SCHOOL,
            UserLevel.SPROUT, true);
        ReissueResult result = new ReissueResult(new TokenPair("new-access", "new-refresh"), user);
        given(tokenService.reissueWithUser("valid-refresh")).willReturn(result);

        MockHttpServletResponse response = new MockHttpServletResponse();
        ResponseEntity<ApiResponse<ReissueResponse>> entity = controller.reissue("valid-refresh", response);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReissueResponse body = entity.getBody().getData();
        assertThat(body.accessToken()).isEqualTo("new-access");
        assertThat(body.tokenType()).isEqualTo("Bearer");
        assertThat(body.userId()).isEqualTo(1L);
        assertThat(body.nickname()).isEqualTo("닉네임123");
        assertThat(body.userType()).isEqualTo(UserType.HIGH_SCHOOL);
        assertThat(body.level()).isEqualTo(UserLevel.SPROUT);
        assertThat(body.onboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("재발급 성공 시 새 refreshToken이 HttpOnly 쿠키로 설정된다")
    void reissue_새_refreshToken_쿠키_설정() {
        User user = mockUser(1L, "test@test.com", "닉네임123", null, null, false);
        ReissueResult result = new ReissueResult(new TokenPair("new-access", "new-refresh"), user);
        given(tokenService.reissueWithUser(anyString())).willReturn(result);

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.reissue("valid-refresh", response);

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).anyMatch(h -> h.startsWith("refreshToken=new-refresh"));
        assertThat(cookies).anyMatch(h ->
            h.startsWith("refreshToken") && h.contains("HttpOnly"));
    }

    @Test
    @DisplayName("재발급 성공 시 응답에 X-Reissued: true 헤더가 포함된다")
    void reissue_성공_시_X_Reissued_헤더_포함() {
        User user = mockUser(1L, "test@test.com", "닉네임123", null, null, false);
        ReissueResult result = new ReissueResult(new TokenPair("new-access", "new-refresh"), user);
        given(tokenService.reissueWithUser(anyString())).willReturn(result);

        MockHttpServletResponse response = new MockHttpServletResponse();
        ResponseEntity<ApiResponse<ReissueResponse>> entity = controller.reissue("valid-refresh", response);

        assertThat(entity.getHeaders().getFirst("X-Reissued")).isEqualTo("true");
    }

    @Test
    @DisplayName("refreshToken 쿠키가 없으면 BadRequestException(AUTH-005)을 던진다")
    void reissue_토큰_없으면_예외() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> controller.reissue(null, response))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex ->
                assertThat(((BadRequestException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TOKEN_MISSING));
    }

    private User mockUser(Long id, String email, String nickname,
                          UserType userType, UserLevel level, boolean onboardingCompleted) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        given(user.getEmail()).willReturn(email);
        given(user.getRole()).willReturn(UserRole.USER);
        given(user.getNickname()).willReturn(nickname);
        given(user.getUserType()).willReturn(userType);
        given(user.getLevel()).willReturn(level);
        given(user.isOnboardingCompleted()).willReturn(onboardingCompleted);
        return user;
    }
}

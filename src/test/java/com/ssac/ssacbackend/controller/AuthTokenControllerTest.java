package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.AuthCodeResult;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.request.AuthCodeExchangeRequest;
import com.ssac.ssacbackend.dto.response.AuthTokenResponse;
import com.ssac.ssacbackend.service.AuthCodeService;
import com.ssac.ssacbackend.service.ReissueResult;
import com.ssac.ssacbackend.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("AuthTokenController")
class AuthTokenControllerTest {

    private AuthCodeService authCodeService;
    private TokenService tokenService;
    private CookieProperties cookieProperties;
    private AuthTokenController controller;

    @BeforeEach
    void setUp() {
        authCodeService = mock(AuthCodeService.class);
        tokenService = mock(TokenService.class);
        cookieProperties = mock(CookieProperties.class);
        controller = new AuthTokenController(authCodeService, tokenService, cookieProperties);
    }

    // ── 기존 회원 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("기존 회원 교환")
    class ExistingUser {

        @Test
        @DisplayName("유효한 authCode이면 accessToken, refreshToken을 포함한 200을 반환하고 refreshToken 쿠키를 설정한다")
        void returnsTokensForExistingUser() {
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);
            User user = User.builder()
                .email("user@example.com")
                .nickname("tester")
                .build();
            given(authCodeService.consume("the-code"))
                .willReturn(Optional.of(AuthCodeResult.existingUser(10L)));
            given(tokenService.issueTokensByUserIdWithUser(10L))
                .willReturn(new ReissueResult(new TokenPair("at", "rt"), user));

            ResponseEntity<AuthTokenResponse> response =
                controller.exchangeToken(new AuthCodeExchangeRequest("the-code"), httpResponse);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            AuthTokenResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.isNewUser()).isFalse();
            assertThat(body.accessToken()).isEqualTo("at");
            assertThat(body.refreshToken()).isEqualTo("rt");
            assertThat(body.tokenType()).isEqualTo("Bearer");
            verify(httpResponse).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
        }

        @Test
        @DisplayName("authCode에 해당하는 userId의 사용자가 없으면 NotFoundException을 던진다")
        void throwsNotFoundWhenUserMissing() {
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);
            given(authCodeService.consume("the-code"))
                .willReturn(Optional.of(AuthCodeResult.existingUser(99L)));
            given(tokenService.issueTokensByUserIdWithUser(99L))
                .willThrow(new NotFoundException(com.ssac.ssacbackend.common.exception.ErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() ->
                controller.exchangeToken(new AuthCodeExchangeRequest("the-code"), httpResponse))
                .isInstanceOf(NotFoundException.class);
        }
    }

    // ── 신규 회원 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("신규 회원 교환")
    class NewUser {

        @Test
        @DisplayName("신규 회원 authCode이면 tempToken, provider를 포함한 200을 반환한다")
        void returnsTempTokenForNewUser() {
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);
            given(authCodeService.consume("the-code"))
                .willReturn(Optional.of(AuthCodeResult.newUser("my-temp-token", "NAVER")));

            ResponseEntity<AuthTokenResponse> response =
                controller.exchangeToken(new AuthCodeExchangeRequest("the-code"), httpResponse);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            AuthTokenResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.isNewUser()).isTrue();
            assertThat(body.tempToken()).isEqualTo("my-temp-token");
            assertThat(body.provider()).isEqualTo("NAVER");
            assertThat(body.accessToken()).isNull();
            assertThat(body.refreshToken()).isNull();
        }
    }

    // ── 유효하지 않은 코드 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("유효하지 않은 authCode")
    class InvalidCode {

        @Test
        @DisplayName("만료되거나 미존재 authCode이면 BadRequestException을 던진다")
        void throwsBadRequestForInvalidCode() {
            HttpServletResponse httpResponse = mock(HttpServletResponse.class);
            given(authCodeService.consume("bad-code")).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                controller.exchangeToken(new AuthCodeExchangeRequest("bad-code"), httpResponse))
                .isInstanceOf(BadRequestException.class);
        }
    }
}

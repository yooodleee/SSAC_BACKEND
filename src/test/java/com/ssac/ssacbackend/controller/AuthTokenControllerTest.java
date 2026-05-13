package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.dto.AuthCodeResult;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.request.AuthCodeExchangeRequest;
import com.ssac.ssacbackend.dto.response.AuthTokenResponse;
import com.ssac.ssacbackend.service.AuthCodeService;
import com.ssac.ssacbackend.service.TokenService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("AuthTokenController")
class AuthTokenControllerTest {

    private AuthCodeService authCodeService;
    private TokenService tokenService;
    private AuthTokenController controller;

    @BeforeEach
    void setUp() {
        authCodeService = mock(AuthCodeService.class);
        tokenService = mock(TokenService.class);
        controller = new AuthTokenController(authCodeService, tokenService);
    }

    // ── 기존 회원 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("기존 회원 교환")
    class ExistingUser {

        @Test
        @DisplayName("유효한 authCode이면 accessToken, refreshToken을 포함한 200을 반환한다")
        void returnsTokensForExistingUser() {
            given(authCodeService.consume("the-code"))
                .willReturn(Optional.of(AuthCodeResult.existingUser(10L)));
            given(tokenService.issueTokensByUserId(10L)).willReturn(new TokenPair("at", "rt"));

            ResponseEntity<AuthTokenResponse> response =
                controller.exchangeToken(new AuthCodeExchangeRequest("the-code"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            AuthTokenResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.isNewUser()).isFalse();
            assertThat(body.accessToken()).isEqualTo("at");
            assertThat(body.refreshToken()).isEqualTo("rt");
            assertThat(body.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("authCode에 해당하는 userId의 사용자가 없으면 NotFoundException을 던진다")
        void throwsNotFoundWhenUserMissing() {
            given(authCodeService.consume("the-code"))
                .willReturn(Optional.of(AuthCodeResult.existingUser(99L)));
            given(tokenService.issueTokensByUserId(99L))
                .willThrow(new NotFoundException(com.ssac.ssacbackend.common.exception.ErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() ->
                controller.exchangeToken(new AuthCodeExchangeRequest("the-code")))
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
            given(authCodeService.consume("the-code"))
                .willReturn(Optional.of(AuthCodeResult.newUser("my-temp-token", "NAVER")));

            ResponseEntity<AuthTokenResponse> response =
                controller.exchangeToken(new AuthCodeExchangeRequest("the-code"));

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
            given(authCodeService.consume("bad-code")).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                controller.exchangeToken(new AuthCodeExchangeRequest("bad-code")))
                .isInstanceOf(BadRequestException.class);
        }
    }
}

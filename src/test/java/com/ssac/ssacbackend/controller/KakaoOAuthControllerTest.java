package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * KakaoOAuthController 단위 테스트.
 *
 * <p>실제 요청은 Spring Security OAuth2 필터가 처리하므로
 * 이 컨트롤러의 메서드가 직접 호출되면 UnsupportedOperationException을 던지는지만 검증한다.
 */
@DisplayName("KakaoOAuthController")
class KakaoOAuthControllerTest {

    private KakaoOAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new KakaoOAuthController();
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login - 카카오 로그인 시작 (Swagger 문서화용 더미)")
    class Login {

        @Test
        @DisplayName("login 메서드는 직접 호출 시 UnsupportedOperationException을 던진다")
        void login_직접호출시_예외발생() {
            assertThatThrownBy(() -> controller.login(null))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("redirectTo 파라미터가 있어도 UnsupportedOperationException을 던진다")
        void login_redirectTo있어도_예외발생() {
            assertThatThrownBy(() -> controller.login("/my-page"))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ── callback ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("callback - 카카오 로그인 콜백 (Swagger 문서화용 더미)")
    class Callback {

        @Test
        @DisplayName("callback 메서드는 직접 호출 시 UnsupportedOperationException을 던진다")
        void callback_직접호출시_예외발생() {
            assertThatThrownBy(() -> controller.callback("code", "state"))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}

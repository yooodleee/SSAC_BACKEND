package com.ssac.ssacbackend.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssac.ssacbackend.config.CookieProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieUtilsTest {

    private CookieProperties props;

    @BeforeEach
    void setUp() {
        props = new CookieProperties();
        props.setSecure(false);
        props.setSameSite("Lax");
    }

    @Test
    @DisplayName("addAccessTokenCookie는 Set-Cookie 헤더에 accessToken을 설정한다")
    void addAccessTokenCookie_헤더_설정() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.addAccessTokenCookie(response, "accessValue", props);

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("accessToken=accessValue");
        assertThat(header).contains("HttpOnly");
        assertThat(header).contains("Path=/");
    }

    @Test
    @DisplayName("addRefreshTokenCookie는 Set-Cookie 헤더에 refreshToken을 설정한다")
    void addRefreshTokenCookie_헤더_설정() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.addRefreshTokenCookie(response, "refreshValue", props);

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("refreshToken=refreshValue");
        assertThat(header).contains("Path=/api/v1/auth");
    }

    @Test
    @DisplayName("clearAccessTokenCookie는 accessToken을 빈 값·maxAge=0으로 만료시킨다")
    void clearAccessTokenCookie_만료() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.clearAccessTokenCookie(response, props);

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("accessToken=");
        assertThat(header).contains("Max-Age=0");
    }

    @Test
    @DisplayName("clearRefreshTokenCookie는 refreshToken을 빈 값·maxAge=0으로 만료시킨다")
    void clearRefreshTokenCookie_만료() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.clearRefreshTokenCookie(response, props);

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("refreshToken=");
        assertThat(header).contains("Max-Age=0");
    }

    @Test
    @DisplayName("addGuestIdCookie는 Set-Cookie 헤더에 guestId를 설정한다")
    void addGuestIdCookie_헤더_설정() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.addGuestIdCookie(response, "guest-123", props);

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("guestId=guest-123");
    }

    @Test
    @DisplayName("clearGuestIdCookie는 guestId를 빈 값·maxAge=0으로 만료시킨다")
    void clearGuestIdCookie_만료() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.clearGuestIdCookie(response, props);

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("guestId=");
        assertThat(header).contains("Max-Age=0");
    }

    @Test
    @DisplayName("addRedirectToCookie는 Set-Cookie 헤더에 redirectTo를 설정한다")
    void addRedirectToCookie_헤더_설정() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.addRedirectToCookie(response, "/home", props);

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("redirectTo=");
    }

    @Test
    @DisplayName("clearRedirectToCookie는 redirectTo를 빈 값·maxAge=0으로 만료시킨다")
    void clearRedirectToCookie_만료() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.clearRedirectToCookie(response, props);

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("redirectTo=");
        assertThat(header).contains("Max-Age=0");
    }
}

package com.ssac.ssacbackend.common.util;

import com.ssac.ssacbackend.config.CookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * HttpOnly + SameSite + Secure 속성을 일관되게 적용하는 쿠키 유틸리티.
 *
 * <p>Jakarta Cookie API는 SameSite를 지원하지 않으므로 Spring의 ResponseCookie를 사용한다.
 */
public final class CookieUtils {

    public static final int ACCESS_TOKEN_MAX_AGE = 30 * 60;
    public static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;
    public static final int GUEST_ID_MAX_AGE = 30 * 24 * 60 * 60;

    private CookieUtils() {}

    public static void addAccessTokenCookie(
        HttpServletResponse response, String token, CookieProperties props) {
        addCookie(response, "accessToken", token, "/", ACCESS_TOKEN_MAX_AGE, props);
    }

    public static void addRefreshTokenCookie(
        HttpServletResponse response, String token, CookieProperties props) {
        addCookie(response, "refreshToken", token, "/api/v1/auth", REFRESH_TOKEN_MAX_AGE, props);
    }

    public static void clearAccessTokenCookie(HttpServletResponse response, CookieProperties props) {
        addCookie(response, "accessToken", "", "/", 0, props);
    }

    public static void clearRefreshTokenCookie(HttpServletResponse response, CookieProperties props) {
        addCookie(response, "refreshToken", "", "/api/v1/auth", 0, props);
    }

    public static void addGuestIdCookie(HttpServletResponse response, String guestId, CookieProperties props) {
        addCookie(response, "guestId", guestId, "/", GUEST_ID_MAX_AGE, props);
    }

    public static void clearGuestIdCookie(HttpServletResponse response, CookieProperties props) {
        addCookie(response, "guestId", "", "/", 0, props);
    }

    private static void addCookie(
        HttpServletResponse response,
        String name, String value, String path, long maxAge,
        CookieProperties props) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(props.isSecure())
            .path(path)
            .maxAge(maxAge)
            .sameSite(props.getSameSite())
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

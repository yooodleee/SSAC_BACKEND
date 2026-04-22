package com.ssac.ssacbackend.dto;

/**
 * Access Token과 Refresh Token 쌍을 담는 내부 전달 객체.
 *
 * <p>Access Token은 응답 바디에 포함되고, Refresh Token은 HttpOnly 쿠키로 전달된다.
 */
public record TokenPair(String accessToken, String refreshToken) {
}

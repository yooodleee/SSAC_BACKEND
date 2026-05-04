package com.ssac.ssacbackend.dto;

/**
 * 네이버 OAuth 로그인 처리 결과.
 *
 * <p>신규 회원이면 {@code isNewUser=true}이고 {@code tempToken}이 채워진다.
 * 기존 회원이면 {@code isNewUser=false}이고 {@code tokenPair}가 채워진다.
 */
public record NaverLoginResult(
    boolean isNewUser,
    TokenPair tokenPair,
    String tempToken
) {
    public static NaverLoginResult existingUser(TokenPair tokenPair) {
        return new NaverLoginResult(false, tokenPair, null);
    }

    public static NaverLoginResult newUser(String tempToken) {
        return new NaverLoginResult(true, null, tempToken);
    }
}

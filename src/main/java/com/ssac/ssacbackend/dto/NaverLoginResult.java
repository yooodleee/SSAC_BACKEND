package com.ssac.ssacbackend.dto;

/**
 * 네이버 OAuth 로그인 처리 결과.
 *
 * <p>신규 회원이면 {@code isNewUser=true}이고 {@code tempToken}이 채워진다.
 * 기존 회원이면 {@code isNewUser=false}이고 {@code userId}가 채워진다.
 *
 * <p>JWT 발급은 이 결과를 받은 컨트롤러가 {@code AuthCodeService}를 통해
 * 일회용 authCode를 발급한 뒤, FE의 토큰 교환 요청 시점에 수행된다.
 */
public record NaverLoginResult(
    boolean isNewUser,
    Long userId,
    String tempToken
) {
    public static NaverLoginResult existingUser(Long userId) {
        return new NaverLoginResult(false, userId, null);
    }

    public static NaverLoginResult newUser(String tempToken) {
        return new NaverLoginResult(true, null, tempToken);
    }
}

package com.ssac.ssacbackend.dto;

import org.springframework.lang.Nullable;

/**
 * {@link com.ssac.ssacbackend.service.AuthCodeService#consume}의 결과를 담는 DTO.
 *
 * <ul>
 *   <li>기존 회원: {@code newUser=false}, {@code userId} 채워짐</li>
 *   <li>신규 회원: {@code newUser=true}, {@code tempToken}/{@code provider} 채워짐</li>
 * </ul>
 *
 * <p>Controller 레이어가 {@code domain} 패키지에 직접 접근하지 않도록
 * {@code AuthCodeService}가 내부 도메인 객체 대신 이 DTO를 반환한다.
 */
public record AuthCodeResult(

    boolean newUser,

    @Nullable Long userId,

    @Nullable String tempToken,

    @Nullable String provider

) {

    public static AuthCodeResult existingUser(Long userId) {
        return new AuthCodeResult(false, userId, null, null);
    }

    public static AuthCodeResult newUser(String tempToken, String provider) {
        return new AuthCodeResult(true, null, tempToken, provider);
    }
}

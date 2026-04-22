package com.ssac.ssacbackend.dto.response;

import java.util.Map;

/**
 * 카카오 OAuth2 사용자 정보 파싱 클래스.
 */
public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    @SuppressWarnings("unchecked")
    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.profile = (kakaoAccount != null) ? (Map<String, Object>) kakaoAccount.get("profile") : null;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getEmail() {
        return (kakaoAccount != null) ? (String) kakaoAccount.get("email") : null;
    }

    @Override
    public String getNickname() {
        return (profile != null) ? (String) profile.get("nickname") : null;
    }
}

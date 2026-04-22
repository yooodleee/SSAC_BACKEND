package com.ssac.ssacbackend.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KakaoUserInfoTest {

    @Test
    @DisplayName("카카오 API 응답 맵에서 사용자 정보를 정확하게 추출한다")
    void parseKakaoAttributes() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 123456789L);

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");

        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", "테스트유저");
        kakaoAccount.put("profile", profile);

        attributes.put("kakao_account", kakaoAccount);

        // when
        KakaoUserInfo userInfo = new KakaoUserInfo(attributes);

        // then
        assertThat(userInfo.getProviderId()).isEqualTo("123456789");
        assertThat(userInfo.getProvider()).isEqualTo("kakao");
        assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
        assertThat(userInfo.getNickname()).isEqualTo("테스트유저");
    }
}

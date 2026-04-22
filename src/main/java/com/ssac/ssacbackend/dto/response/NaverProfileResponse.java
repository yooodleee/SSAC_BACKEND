package com.ssac.ssacbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 네이버 사용자 프로필 API 응답 DTO.
 *
 * <p>네이버 프로필 엔드포인트(https://openapi.naver.com/v1/nid/me)의 응답을 역직렬화한다.
 * 응답의 response 필드에 실제 사용자 정보가 담긴다.
 */
@Getter
@NoArgsConstructor
public class NaverProfileResponse {

    private String resultcode;
    private String message;
    private NaverUserDetail response;

    /**
     * 네이버 사용자 상세 정보.
     */
    @Getter
    @NoArgsConstructor
    public static class NaverUserDetail {

        private String id;
        private String email;
        private String nickname;
        private String name;

        @JsonProperty("profile_image")
        private String profileImage;
    }
}

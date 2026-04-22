package com.ssac.ssacbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 네이버 OAuth 토큰 교환 응답 DTO.
 *
 * <p>네이버 토큰 엔드포인트(https://nid.naver.com/oauth2.0/token)의 응답을 역직렬화한다.
 */
@Getter
@NoArgsConstructor
public class NaverTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private String expiresIn;

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;
}

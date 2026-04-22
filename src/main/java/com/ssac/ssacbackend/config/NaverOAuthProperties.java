package com.ssac.ssacbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 네이버 OAuth 설정 프로퍼티.
 *
 * <p>application.properties의 naver.oauth.* 값을 바인딩한다.
 * 운영 환경에서는 환경 변수로 주입해야 한다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "naver.oauth")
public class NaverOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
}

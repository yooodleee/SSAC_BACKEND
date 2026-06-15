package com.ssac.ssacbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 네이버 OAuth 관련 빈 설정.
 *
 * <p>NaverOAuthProperties를 활성화하고 RestClient 빈을 등록한다.
 */
@Configuration
@EnableConfigurationProperties(NaverOAuthProperties.class)
public class NaverOAuthConfig {

    /**
     * 네이버 API 호출에 사용하는 RestClient 빈.
     */
    @Bean
    public RestClient naverRestClient() {
        return RestClient.create();
    }
}

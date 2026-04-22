package com.ssac.ssacbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 프로퍼티.
 *
 * <p>application.properties의 jwt.* 값을 바인딩한다.
 * 운영 환경에서는 jwt.secret을 환경 변수(JWT_SECRET)로 주입해야 한다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs;
    private long refreshExpirationMs;
}

package com.ssac.ssacbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 쿠키 보안 설정 프로퍼티.
 *
 * <p>운영 환경(HTTPS)에서는 COOKIE_SECURE=true 환경 변수를 주입해야 한다.
 * SameSite=Lax는 CSRF를 방어하면서 OAuth 리다이렉트(GET)를 허용한다.
 */
@ConfigurationProperties(prefix = "cookie")
@Getter
@Setter
public class CookieProperties {

    /** 운영 환경(HTTPS)에서 true로 설정해야 한다. */
    private boolean secure = false;

    /**
     * SameSite 속성 값. Lax(기본) | Strict | None.
     *
     * <p>None 사용 시 secure=true가 필수이다.
     */
    private String sameSite = "Lax";
}

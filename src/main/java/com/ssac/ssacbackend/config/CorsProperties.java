package com.ssac.ssacbackend.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 출처 설정 프로퍼티.
 *
 * <p>cors.allowed-origins에 프론트엔드 URL을 콤마 구분 목록으로 지정한다.
 * 운영 환경에서는 환경 변수 CORS_ALLOWED_ORIGINS로 주입한다.
 */
@ConfigurationProperties(prefix = "cors")
@Getter
@Setter
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:3000");
}

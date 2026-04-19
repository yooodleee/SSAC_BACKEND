package com.ssac.ssacbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정.
 *
 * <p>공개 경로:
 * - /api-docs/**     : OpenAPI JSON (에이전트/프론트엔드 계약 문서)
 * - /swagger-ui/**   : Swagger UI 리소스
 * - /swagger-ui.html : Swagger UI 진입점
 *
 * <p>나머지 모든 경로는 JWT Bearer 토큰 인증이 필요하다.
 * JWT 필터는 추후 JwtAuthenticationFilter 구현 후 addFilterBefore()로 등록한다.
 *
 * <p>변경 기준: docs/decisions/004-swagger-contract.md
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated()
            )
            .build();
    }
}

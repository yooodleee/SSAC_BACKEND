package com.ssac.ssacbackend.config;

import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.CustomOAuth2UserService;
import com.ssac.ssacbackend.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정.
 *
 * <p>JWT 기반 Stateless 인증과 OAuth2 소셜 로그인을 함께 지원한다.
 * Rate Limiting 필터를 JWT 필터 앞에 배치하여 OAuth 엔드포인트를 보호한다.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, CookieProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final RateLimitingFilter rateLimitingFilter;

    private static final String[] PUBLIC_PATHS = {
        "/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/api/v1/auth/**",
        "/login/**",
        "/oauth2/**",
        "/api/news"
    };

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http, JwtService jwtService, UserRepository userRepository)
        throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                // 관리자 전용: 사용자 목록 조회 및 권한 관리
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // 퀴즈 제출: 비회원(GUEST)도 허용
                .requestMatchers(HttpMethod.POST, "/api/v1/quiz-attempts")
                    .hasAnyRole("USER", "GUEST", "ADMIN")
                // 퀴즈 기록/통계 조회: 로그인 회원만 허용
                .requestMatchers(HttpMethod.GET, "/api/v1/quiz-attempts", "/api/v1/quiz-attempts/**")
                    .hasAnyRole("USER", "ADMIN")
                // 개인화 추천: 로그인 회원만 허용 (GUEST 차단)
                .requestMatchers(HttpMethod.GET, "/api/v1/recommendations")
                    .hasAnyRole("USER", "ADMIN")
                // 알림/이어보기/세그먼트: 로그인 회원만 허용
                .requestMatchers("/api/notification/**", "/api/resume/**", "/api/user/segment")
                    .hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((req, res, e) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.getWriter().write(
                        "{\"status\":403,\"code\":\"FORBIDDEN\","
                        + "\"message\":\"접근 권한이 없습니다.\"}");
                })
                .authenticationEntryPoint((req, res, e) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().write(
                        "{\"status\":401,\"code\":\"UNAUTHORIZED\","
                        + "\"message\":\"인증이 필요합니다.\"}");
                })
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtService, userRepository),
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }

    /**
     * RateLimitingFilter를 Spring의 서블릿 필터 체인에 자동 등록하지 않도록 비활성화한다.
     *
     * <p>Security 필터 체인에서만 명시적으로 관리한다.
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(
        RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

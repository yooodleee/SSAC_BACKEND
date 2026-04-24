package com.ssac.ssacbackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssac.ssacbackend.service.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-32-chars!");
        props.setExpirationMs(1_800_000L);
        props.setRefreshExpirationMs(604_800_000L);
        jwtService = new JwtService(props);
        filter = new JwtAuthenticationFilter(jwtService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GUEST 토큰이면 SecurityContext에 guestId를 principal로, ROLE_GUEST를 authority로 설정한다")
    void doFilter_guestToken_setsGuestRoleAndGuestIdAsPrincipal() throws Exception {
        String guestId = UUID.randomUUID().toString();
        String token = jwtService.generateGuestToken(guestId);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(guestId);
        assertThat(auth.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_GUEST");
    }

    @Test
    @DisplayName("USER 토큰이면 SecurityContext에 email을 principal로, ROLE_USER를 authority로 설정한다")
    void doFilter_userToken_setsUserRoleAndEmailAsPrincipal() throws Exception {
        String token = jwtService.generateAccessToken(1L, "user@test.com", "USER");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("user@test.com");
        assertThat(auth.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("ADMIN 토큰이면 SecurityContext에 ROLE_ADMIN을 설정한다")
    void doFilter_adminToken_setsAdminRole() throws Exception {
        String token = jwtService.generateAccessToken(2L, "admin@test.com", "ADMIN");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("토큰이 없으면 SecurityContext에 인증 정보를 설정하지 않는다")
    void doFilter_noToken_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 SecurityContext에 인증 정보를 설정하지 않는다")
    void doFilter_invalidToken_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer totally.invalid.token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("accessToken 쿠키에 GUEST 토큰이 있어도 동일하게 ROLE_GUEST를 설정한다")
    void doFilter_guestTokenInCookie_setsGuestRole() throws Exception {
        String guestId = UUID.randomUUID().toString();
        String token = jwtService.generateGuestToken(guestId);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("accessToken", token));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(guestId);
        assertThat(auth.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_GUEST");
    }
}

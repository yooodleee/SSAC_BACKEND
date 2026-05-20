package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ssac.ssacbackend.dto.response.AuthStatusResponse;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AuthStatusControllerTest {

    private final AuthStatusController controller = new AuthStatusController();

    @Test
    @DisplayName("비로그인 시 isAuthenticated: false")
    void 비로그인_상태_응답() {
        ResponseEntity<?> response = controller.getStatus(null);
        AuthStatusResponse body = (AuthStatusResponse) ((com.ssac.ssacbackend.common.response.ApiResponse<?>) response.getBody()).getData();

        assertThat(body.isAuthenticated()).isFalse();
        assertThat(body.role()).isNull();
    }

    @Test
    @DisplayName("일반 사용자 로그인 시 role: USER, redirectTo: /home")
    void 일반_사용자_로그인_상태_응답() {
        Authentication auth = mockAuth("user@test.com", "USER");
        ResponseEntity<?> response = controller.getStatus(auth);
        AuthStatusResponse body = (AuthStatusResponse) ((com.ssac.ssacbackend.common.response.ApiResponse<?>) response.getBody()).getData();

        assertThat(body.isAuthenticated()).isTrue();
        assertThat(body.role()).isEqualTo("USER");
        assertThat(body.redirectTo()).isEqualTo("/home");
    }

    @Test
    @DisplayName("관리자 로그인 시 role: ADMIN, redirectTo: /admin")
    void 관리자_로그인_시_role_ADMIN_포함() {
        Authentication auth = mockAuth("admin@test.com", "ADMIN");
        ResponseEntity<?> response = controller.getStatus(auth);
        AuthStatusResponse body = (AuthStatusResponse) ((com.ssac.ssacbackend.common.response.ApiResponse<?>) response.getBody()).getData();

        assertThat(body.isAuthenticated()).isTrue();
        assertThat(body.role()).isEqualTo("ADMIN");
        assertThat(body.redirectTo()).isEqualTo("/admin");
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Authentication mockAuth(String name, String role) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(name);
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        when(auth.getAuthorities()).thenReturn((Collection) authorities);
        return auth;
    }
}

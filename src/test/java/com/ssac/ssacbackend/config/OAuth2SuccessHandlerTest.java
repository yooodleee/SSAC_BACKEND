package com.ssac.ssacbackend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.service.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class OAuth2SuccessHandlerTest {

    @Test
    @DisplayName("인증 성공 시 JWT 토큰을 쿠키에 담고 루트 경로로 리다이렉트한다")
    void onAuthenticationSuccessSetsCookieAndRedirects() throws IOException {
        // given
        JwtService jwtService = mock(JwtService.class);
        given(jwtService.generateToken(anyString())).willReturn("test-jwt-token");

        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(jwtService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Authentication authentication = mock(Authentication.class);
        OAuth2User oAuth2User = mock(OAuth2User.class);
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "12345");
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");
        attributes.put("kakao_account", kakaoAccount);
        
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(oAuth2User.getAttributes()).willReturn(attributes);

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        Cookie cookie = response.getCookie("accessToken");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("test-jwt-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }
}

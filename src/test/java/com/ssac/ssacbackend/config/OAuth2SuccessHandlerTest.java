package com.ssac.ssacbackend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.TokenService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

class OAuth2SuccessHandlerTest {

    @Test
    @DisplayName("인증 성공 시 accessToken·refreshToken 쿠키를 설정하고 루트 경로로 리다이렉트한다")
    void onAuthenticationSuccess_setsCookiesAndRedirects() throws IOException {
        TokenService tokenService = mock(TokenService.class);
        UserRepository userRepository = mock(UserRepository.class);

        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");

        OAuth2SuccessHandler handler =
            new OAuth2SuccessHandler(tokenService, userRepository, cookieProperties);

        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userRepository.findByProviderAndProviderId("kakao", "12345")).willReturn(Optional.of(user));
        given(tokenService.issueTokens(any(User.class)))
            .willReturn(new TokenPair("access-token", "refresh-token"));

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

        handler.onAuthenticationSuccess(request, response, authentication);

        List<String> setCookieHeaders = response.getHeaders("Set-Cookie");
        assertThat(setCookieHeaders).anyMatch(h -> h.startsWith("accessToken=access-token"));
        assertThat(setCookieHeaders).anyMatch(h -> h.startsWith("refreshToken=refresh-token"));
        assertThat(setCookieHeaders).allMatch(h -> h.contains("HttpOnly"));
        assertThat(setCookieHeaders).allMatch(h -> h.contains("SameSite=Lax"));
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }
}

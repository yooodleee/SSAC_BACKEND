package com.ssac.ssacbackend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.GuestMigrationService;
import com.ssac.ssacbackend.service.TokenService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

class OAuth2SuccessHandlerTest {

    private TokenService tokenService;
    private GuestMigrationService guestMigrationService;
    private UserRepository userRepository;
    private OAuth2SuccessHandler handler;
    private User user;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        guestMigrationService = mock(GuestMigrationService.class);
        userRepository = mock(UserRepository.class);

        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");

        handler = new OAuth2SuccessHandler(tokenService, guestMigrationService, userRepository, cookieProperties);

        user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userRepository.findByProviderAndProviderId("kakao", "12345")).willReturn(Optional.of(user));
        given(tokenService.issueTokens(any(User.class)))
            .willReturn(new TokenPair("access-token", "refresh-token"));
    }

    @Test
    @DisplayName("인증 성공 시 FE 콜백 URL로 리다이렉트한다")
    void onAuthenticationSuccessSetsCookiesAndRedirects() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, buildAuthentication());

        assertThat(response.getRedirectedUrl())
            .contains("/auth/kakao/callback?token=access-token");
    }

    @Test
    @DisplayName("guestId 쿠키가 없으면 마이그레이션을 호출하지 않는다")
    void onAuthenticationSuccessNoGuestIdCookieDoesNotMigrate() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, buildAuthentication());

        verify(guestMigrationService, never()).migrateGuestData(any(), any());
    }

    @Test
    @DisplayName("guestId 쿠키가 있으면 마이그레이션을 실행하고 guestId 쿠키를 삭제한다")
    void onAuthenticationSuccessWithGuestIdCookieMigratesAndClearsCookie() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("guestId", "test-guest-uuid"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, buildAuthentication());

        verify(guestMigrationService).migrateGuestData(eq("test-guest-uuid"), eq(user));
        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).anyMatch(h -> h.startsWith("guestId=") && h.contains("Max-Age=0"));
    }

    private Authentication buildAuthentication() {
        Authentication authentication = mock(Authentication.class);
        OAuth2User oAuth2User = mock(OAuth2User.class);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "12345");
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");
        attributes.put("kakao_account", kakaoAccount);

        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(oAuth2User.getAttributes()).willReturn(attributes);
        return authentication;
    }
}

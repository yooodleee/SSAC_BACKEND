package com.ssac.ssacbackend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.AuthCodeService;
import com.ssac.ssacbackend.service.GuestMigrationService;
import com.ssac.ssacbackend.service.PendingRegistrationService;
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
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2SuccessHandlerTest {

    private AuthCodeService authCodeService;
    private GuestMigrationService guestMigrationService;
    private UserRepository userRepository;
    private PendingRegistrationService pendingRegistrationService;
    private OAuth2SuccessHandler handler;
    private User user;

    @BeforeEach
    void setUp() {
        authCodeService = mock(AuthCodeService.class);
        guestMigrationService = mock(GuestMigrationService.class);
        userRepository = mock(UserRepository.class);
        pendingRegistrationService = mock(PendingRegistrationService.class);

        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");

        handler = new OAuth2SuccessHandler(
            authCodeService, guestMigrationService, userRepository, pendingRegistrationService, cookieProperties);
        ReflectionTestUtils.setField(handler, "defaultRedirectUri", "http://localhost:3000");

        user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userRepository.findByProviderAndProviderId("kakao", "12345")).willReturn(Optional.of(user));
        given(authCodeService.issueForExistingUser(1L)).willReturn("auth-code-existing");
    }

    @Test
    @DisplayName("기존 회원 인증 성공 시 authCode와 isNewUser=false를 담아 리다이렉트한다")
    void onAuthenticationSuccessRedirectsToFrontendCallbackWithAuthCode() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, buildAuthentication());

        assertThat(response.getRedirectedUrl())
            .isEqualTo("http://localhost:3000/auth/kakao/callback?authCode=auth-code-existing&isNewUser=false");
    }

    @Test
    @DisplayName("신규 회원은 authCode와 isNewUser=true를 포함한 URL로 리다이렉트한다 (tempToken은 URL에 노출되지 않는다)")
    void onAuthenticationSuccessNewUserRedirectsWithAuthCode() throws IOException {
        given(userRepository.findByProviderAndProviderId("kakao", "12345")).willReturn(Optional.empty());
        given(pendingRegistrationService.create(eq(OAuthProvider.KAKAO), eq("12345"), any()))
            .willReturn("temp-token-123");
        given(authCodeService.issueForNewUser("temp-token-123", OAuthProvider.KAKAO))
            .willReturn("auth-code-new");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, buildAuthentication());

        assertThat(response.getRedirectedUrl())
            .isEqualTo("http://localhost:3000/auth/kakao/callback?authCode=auth-code-new&isNewUser=true");
        assertThat(response.getRedirectedUrl())
            .doesNotContain("tempToken")
            .doesNotContain("token=");
    }

    @Test
    @DisplayName("guestId 쿠키가 없으면 마이그레이션을 호출하지 않는다")
    void onAuthenticationSuccessNoGuestIdCookieDoesNotMigrate() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, buildAuthentication());

        verify(guestMigrationService, never()).migrateGuestData(any(), any());
        verify(authCodeService).issueForExistingUser(1L);
    }

    @Test
    @DisplayName("guestId 쿠키가 있으면 마이그레이션을 실행하고 guestId 쿠키를 삭제한다")
    void onAuthenticationSuccessWithGuestIdCookieMigratesAndClearsCookie() throws IOException {
        given(guestMigrationService.migrateGuestData(any(), any()))
            .willReturn(new GuestMigrationService.MigrationResult(true, 2));

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

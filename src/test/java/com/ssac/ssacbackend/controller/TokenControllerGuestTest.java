package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.config.CookieProperties;
import com.ssac.ssacbackend.dto.response.LoginResponse;
import com.ssac.ssacbackend.service.JwtService;
import com.ssac.ssacbackend.service.TokenService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

class TokenControllerGuestTest {

    private TokenController controller;

    @BeforeEach
    void setUp() {
        TokenService tokenService = mock(TokenService.class);
        JwtService jwtService = mock(JwtService.class);
        given(jwtService.generateGuestToken(anyString())).willReturn("guest-access-token");

        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");

        controller = new TokenController(tokenService, jwtService, cookieProperties);
    }

    @Test
    @DisplayName("POST /auth/guest는 200과 함께 Guest Access Token을 응답 바디에 반환한다")
    void issueGuestToken_returnsAccessTokenInBody() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ApiResponse<LoginResponse>> result = controller.issueGuestToken(response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getData().accessToken()).isEqualTo("guest-access-token");
        assertThat(result.getBody().getData().tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("POST /auth/guest는 accessToken HttpOnly 쿠키를 설정한다")
    void issueGuestToken_setsAccessTokenCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.issueGuestToken(response);

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).anyMatch(h -> h.startsWith("accessToken=guest-access-token"));
        assertThat(cookies).anyMatch(h ->
            h.startsWith("accessToken") && h.contains("HttpOnly"));
    }

    @Test
    @DisplayName("POST /auth/guest는 guestId HttpOnly 쿠키를 설정한다")
    void issueGuestToken_setsGuestIdCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.issueGuestToken(response);

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).anyMatch(h -> h.startsWith("guestId=") && h.contains("HttpOnly"));
    }

    @Test
    @DisplayName("POST /auth/guest는 매 호출마다 다른 guestId 쿠키를 발급한다")
    void issueGuestToken_generatesUniqueGuestIdEachTime() {
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        controller.issueGuestToken(response1);
        controller.issueGuestToken(response2);

        String guestId1 = extractGuestId(response1);
        String guestId2 = extractGuestId(response2);
        assertThat(guestId1).isNotEqualTo(guestId2);
    }

    private String extractGuestId(MockHttpServletResponse response) {
        return response.getHeaders("Set-Cookie").stream()
            .filter(h -> h.startsWith("guestId="))
            .findFirst()
            .map(h -> h.split(";")[0].replace("guestId=", ""))
            .orElse("");
    }
}

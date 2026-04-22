package com.ssac.ssacbackend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import java.io.IOException;

class OAuth2FailureHandlerTest {

    @Test
    @DisplayName("인증 실패 시 에러 메시지를 쿼리 파라미터에 담아 루트 경로로 리다이렉트한다")
    void onAuthenticationFailureRedirectsWithError() throws IOException {
        // given
        OAuth2FailureHandler handler = new OAuth2FailureHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = mock(AuthenticationException.class);
        org.mockito.BDDMockito.given(exception.getMessage()).willReturn("인증 실패 사유");

        // when
        handler.onAuthenticationFailure(request, response, exception);

        // then
        assertThat(response.getRedirectedUrl()).startsWith("/");
        assertThat(response.getRedirectedUrl()).contains("error=");
    }
}

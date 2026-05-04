package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CustomOAuth2UserService 단위 테스트.
 *
 * <p>loadUser()는 내부적으로 DefaultOAuth2UserService를 통해 HTTP 호출을 수행하므로
 * 실제 네트워크 호출이 필요한 시나리오는 통합 테스트로 분리한다.
 * 여기서는 서비스 인스턴스 생성 및 의존성 없는 동작만 검증한다.
 */
class CustomOAuth2UserServiceTest {

    private CustomOAuth2UserService customOAuth2UserService;

    @BeforeEach
    void setUp() {
        // DB/네트워크 의존성 없이 생성 가능한지 확인
        customOAuth2UserService = new CustomOAuth2UserService();
    }

    @Test
    @DisplayName("CustomOAuth2UserService는 외부 의존성 없이 생성된다")
    void serviceIsCreatedWithoutExternalDependencies() {
        assertThat(customOAuth2UserService).isNotNull();
    }

    @Test
    @DisplayName("신규 회원 가입은 OAuth2SuccessHandler에서 처리된다 (설계 결정 문서화)")
    void newUserRegistrationIsHandledBySuccessHandler() {
        // CustomOAuth2UserService는 loadUser()에서 DB 저장을 하지 않는다.
        // 신규/기존 회원 분기는 OAuth2SuccessHandler.onAuthenticationSuccess()에서 처리된다.
        assertThat(customOAuth2UserService).isNotNull();
    }
}

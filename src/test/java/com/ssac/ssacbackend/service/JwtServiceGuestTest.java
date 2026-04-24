package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.service.JwtService.TokenInfo;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceGuestTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-32-chars!");
        props.setExpirationMs(1_800_000L);
        props.setRefreshExpirationMs(604_800_000L);
        jwtService = new JwtService(props);
    }

    @Test
    @DisplayName("generateGuestToken은 GUEST role과 guestId를 sub로 담은 JWT를 생성한다")
    void generateGuestToken_containsGuestRoleAndGuestId() {
        String guestId = UUID.randomUUID().toString();

        String token = jwtService.generateGuestToken(guestId);

        assertThat(token).isNotBlank();
        Optional<TokenInfo> info = jwtService.extractTokenInfoIfValid(token);
        assertThat(info).isPresent();
        assertThat(info.get().principal()).isEqualTo(guestId);
        assertThat(info.get().role()).isEqualTo("GUEST");
    }

    @Test
    @DisplayName("GUEST 토큰에는 email claim이 없어 extractEmailIfValid는 빈 Optional을 반환한다")
    void generateGuestToken_hasNoEmailClaim() {
        String token = jwtService.generateGuestToken(UUID.randomUUID().toString());

        assertThat(jwtService.extractEmailIfValid(token)).isEmpty();
    }

    @Test
    @DisplayName("USER 토큰에서 extractTokenInfoIfValid는 email을 principal로 USER를 role로 반환한다")
    void extractTokenInfoIfValid_userToken_returnsEmailAndUserRole() {
        String token = jwtService.generateAccessToken(1L, "user@test.com", "USER");

        Optional<TokenInfo> info = jwtService.extractTokenInfoIfValid(token);

        assertThat(info).isPresent();
        assertThat(info.get().principal()).isEqualTo("user@test.com");
        assertThat(info.get().role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("ADMIN 토큰에서 extractTokenInfoIfValid는 email을 principal로 ADMIN을 role로 반환한다")
    void extractTokenInfoIfValid_adminToken_returnsEmailAndAdminRole() {
        String token = jwtService.generateAccessToken(2L, "admin@test.com", "ADMIN");

        Optional<TokenInfo> info = jwtService.extractTokenInfoIfValid(token);

        assertThat(info).isPresent();
        assertThat(info.get().principal()).isEqualTo("admin@test.com");
        assertThat(info.get().role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("서명이 다른 유효하지 않은 토큰은 빈 Optional을 반환한다")
    void extractTokenInfoIfValid_invalidToken_returnsEmpty() {
        Optional<TokenInfo> result = jwtService.extractTokenInfoIfValid("invalid.token.value");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("다른 secret으로 서명된 토큰은 빈 Optional을 반환한다")
    void extractTokenInfoIfValid_differentSecretToken_returnsEmpty() {
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("another-secret-key-completely-different-!!");
        otherProps.setExpirationMs(1_800_000L);
        JwtService otherService = new JwtService(otherProps);
        String alienToken = otherService.generateGuestToken(UUID.randomUUID().toString());

        Optional<TokenInfo> result = jwtService.extractTokenInfoIfValid(alienToken);

        assertThat(result).isEmpty();
    }
}

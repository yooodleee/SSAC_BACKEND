package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.dto.AuthCodeResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@DisplayName("AuthCodeService")
@ExtendWith(MockitoExtension.class)
class AuthCodeServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private AuthCodeService authCodeService;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        authCodeService = new AuthCodeService(redisTemplate);
    }

    // ── 기존 회원 발급 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("기존 회원 코드 발급(issueForExistingUser)")
    class IssueForExistingUser {

        @Test
        @DisplayName("UUID 형태의 코드를 반환한다")
        void returnsNonBlankCode() {
            String code = authCodeService.issueForExistingUser(1L);
            assertThat(code).isNotBlank();
        }

        @Test
        @DisplayName("발급마다 서로 다른 코드를 반환한다")
        void returnsUniqueCode() {
            String code1 = authCodeService.issueForExistingUser(1L);
            String code2 = authCodeService.issueForExistingUser(1L);
            assertThat(code1).isNotEqualTo(code2);
        }

        @Test
        @DisplayName("Redis에 올바른 값으로 저장된다")
        void storesInRedis() {
            authCodeService.issueForExistingUser(42L);
            verify(valueOps).set(anyString(), eq("EXISTING:42"), any());
        }
    }

    // ── 신규 회원 발급 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("신규 회원 코드 발급(issueForNewUser)")
    class IssueForNewUser {

        @Test
        @DisplayName("UUID 형태의 코드를 반환한다")
        void returnsNonBlankCode() {
            String code = authCodeService.issueForNewUser("temp-token", OAuthProvider.NAVER);
            assertThat(code).isNotBlank();
        }

        @Test
        @DisplayName("Redis에 올바른 값으로 저장된다")
        void storesInRedis() {
            authCodeService.issueForNewUser("my-token", OAuthProvider.KAKAO);
            verify(valueOps).set(anyString(), eq("NEW:my-token:KAKAO"), any());
        }
    }

    // ── consume ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("코드 소비(consume)")
    class Consume {

        @Test
        @DisplayName("기존 회원 코드 소비 시 isNewUser=false, userId를 반환한다")
        void existingUserCode() {
            given(valueOps.getAndDelete(anyString())).willReturn("EXISTING:42");

            Optional<AuthCodeResult> result = authCodeService.consume("some-code");

            assertThat(result).isPresent();
            assertThat(result.get().newUser()).isFalse();
            assertThat(result.get().userId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("신규 회원 코드 소비 시 isNewUser=true, tempToken, provider를 반환한다")
        void newUserCode() {
            given(valueOps.getAndDelete(anyString())).willReturn("NEW:my-temp-token:KAKAO");

            Optional<AuthCodeResult> result = authCodeService.consume("some-code");

            assertThat(result).isPresent();
            assertThat(result.get().newUser()).isTrue();
            assertThat(result.get().tempToken()).isEqualTo("my-temp-token");
            assertThat(result.get().provider()).isEqualTo("KAKAO");
        }

        @Test
        @DisplayName("존재하지 않는 코드를 소비하면 empty를 반환한다")
        void unknownCode() {
            given(valueOps.getAndDelete(anyString())).willReturn(null);

            Optional<AuthCodeResult> result = authCodeService.consume("non-existent-code");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Redis TTL 만료 후(getAndDelete가 null 반환) empty를 반환한다")
        void expiredCode() {
            given(valueOps.getAndDelete(anyString())).willReturn(null);

            Optional<AuthCodeResult> result = authCodeService.consume("expired-code");

            assertThat(result).isEmpty();
        }
    }
}

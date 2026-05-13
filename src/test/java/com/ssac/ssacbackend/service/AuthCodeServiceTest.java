package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssac.ssacbackend.domain.auth.AuthCode;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.dto.AuthCodeResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("AuthCodeService")
class AuthCodeServiceTest {

    private AuthCodeService authCodeService;

    @BeforeEach
    void setUp() {
        authCodeService = new AuthCodeService();
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
    }

    // ── consume ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("코드 소비(consume)")
    class Consume {

        @Test
        @DisplayName("기존 회원 코드 소비 시 isNewUser=false, userId를 반환한다")
        void existingUserCode() {
            String code = authCodeService.issueForExistingUser(42L);

            Optional<AuthCodeResult> result = authCodeService.consume(code);

            assertThat(result).isPresent();
            assertThat(result.get().newUser()).isFalse();
            assertThat(result.get().userId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("신규 회원 코드 소비 시 isNewUser=true, tempToken, provider를 반환한다")
        void newUserCode() {
            String code = authCodeService.issueForNewUser("my-temp-token", OAuthProvider.KAKAO);

            Optional<AuthCodeResult> result = authCodeService.consume(code);

            assertThat(result).isPresent();
            assertThat(result.get().newUser()).isTrue();
            assertThat(result.get().tempToken()).isEqualTo("my-temp-token");
            assertThat(result.get().provider()).isEqualTo("KAKAO");
        }

        @Test
        @DisplayName("코드를 두 번 소비하면 두 번째는 empty를 반환한다 (1회용)")
        void consumeOnce() {
            String code = authCodeService.issueForExistingUser(1L);

            authCodeService.consume(code);
            Optional<AuthCodeResult> second = authCodeService.consume(code);

            assertThat(second).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 코드를 소비하면 empty를 반환한다")
        void unknownCode() {
            Optional<AuthCodeResult> result = authCodeService.consume("non-existent-code");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("TTL 만료된 코드를 소비하면 empty를 반환한다")
        void expiredCode() {
            String code = authCodeService.issueForExistingUser(1L);

            // createdAt을 TTL보다 이전으로 조작한다
            var store = (java.util.concurrent.ConcurrentHashMap<?, ?>) ReflectionTestUtils
                .getField(authCodeService, "store");
            AuthCode authCode = (AuthCode) store.get(code);
            ReflectionTestUtils.setField(authCode, "createdAt",
                java.time.Instant.now().minusSeconds(AuthCodeService.TTL_SECONDS + 1));

            Optional<AuthCodeResult> result = authCodeService.consume(code);
            assertThat(result).isEmpty();
        }
    }
}

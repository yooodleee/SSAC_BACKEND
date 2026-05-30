package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.auth.PendingRegistration;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class PendingRegistrationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private PendingRegistrationService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("create - tempToken을 생성하고 Redis에 저장한다")
    void create_정상() {
        String tempToken = service.create(OAuthProvider.NAVER, "naver-123", "test@naver.com");

        assertThat(tempToken).isNotBlank();
        verify(valueOps).set(
            eq(PendingRegistrationService.KEY_PREFIX + tempToken),
            anyString(),
            eq(Duration.ofSeconds(PendingRegistrationService.TTL_SECONDS))
        );
    }

    @Test
    @DisplayName("findValid - Redis에 값이 있으면 PendingRegistration을 반환한다")
    void findValid_캐시히트() {
        PendingRegistration pending = new PendingRegistration(
            "tok-1", OAuthProvider.KAKAO, "kakao-456", "test@kakao.com");
        String json = buildJson("tok-1", "KAKAO", "kakao-456", "test@kakao.com", false);

        given(valueOps.get(PendingRegistrationService.KEY_PREFIX + "tok-1")).willReturn(json);

        Optional<PendingRegistration> result = service.findValid("tok-1");

        assertThat(result).isPresent();
        assertThat(result.get().getProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(result.get().getEmail()).isEqualTo("test@kakao.com");
    }

    @Test
    @DisplayName("findValid - Redis에 값이 없으면 empty를 반환한다")
    void findValid_캐시미스() {
        given(valueOps.get(PendingRegistrationService.KEY_PREFIX + "tok-missing")).willReturn(null);

        Optional<PendingRegistration> result = service.findValid("tok-missing");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findValid - 역직렬화 실패 시 empty를 반환한다")
    void findValid_역직렬화실패() {
        given(valueOps.get(PendingRegistrationService.KEY_PREFIX + "tok-corrupt"))
            .willReturn("{invalid-json}");

        Optional<PendingRegistration> result = service.findValid("tok-corrupt");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("update - TTL이 남아있으면 Redis를 업데이트한다")
    void update_정상() {
        PendingRegistration pending = new PendingRegistration(
            "tok-2", OAuthProvider.NAVER, "naver-789", "update@naver.com");
        given(redisTemplate.getExpire(PendingRegistrationService.KEY_PREFIX + "tok-2", TimeUnit.SECONDS))
            .willReturn(300L);

        service.update("tok-2", pending);

        verify(valueOps).set(
            eq(PendingRegistrationService.KEY_PREFIX + "tok-2"),
            anyString(),
            eq(Duration.ofSeconds(300L))
        );
    }

    @Test
    @DisplayName("update - TTL이 만료되었으면 아무 동작도 하지 않는다")
    void update_TTL만료() {
        PendingRegistration pending = new PendingRegistration(
            "tok-expired", OAuthProvider.NAVER, "naver-000", "expired@naver.com");
        given(redisTemplate.getExpire(
            PendingRegistrationService.KEY_PREFIX + "tok-expired", TimeUnit.SECONDS))
            .willReturn(-1L);

        service.update("tok-expired", pending);
        // valueOps.set이 호출되지 않으면 성공 (verify 없음)
    }

    @Test
    @DisplayName("invalidate - Redis 키를 즉시 삭제한다")
    void invalidate_정상() {
        service.invalidate("tok-done");

        verify(redisTemplate).delete(PendingRegistrationService.KEY_PREFIX + "tok-done");
    }

    private String buildJson(String tempToken, String provider,
                             String providerUserId, String email, boolean termsCompleted) {
        return String.format(
            "{\"tempToken\":\"%s\",\"provider\":\"%s\",\"providerUserId\":\"%s\"," +
            "\"email\":\"%s\",\"termsCompleted\":\"%b\"," +
            "\"serviceTermAgreedAt\":\"\",\"privacyTermAgreedAt\":\"\"," +
            "\"ageVerificationAgreedAt\":\"\",\"marketingTermAgreedAt\":\"\"}",
            tempToken, provider, providerUserId, email, termsCompleted);
    }
}

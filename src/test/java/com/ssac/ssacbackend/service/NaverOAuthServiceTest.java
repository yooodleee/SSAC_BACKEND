package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.config.NaverOAuthProperties;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.domain.social.SocialAccount;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.NaverLoginResult;
import com.ssac.ssacbackend.dto.response.NaverProfileResponse;
import com.ssac.ssacbackend.dto.response.NaverTokenResponse;
import com.ssac.ssacbackend.repository.SocialAccountRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NaverOAuthServiceTest {

    @Mock
    private NaverOAuthProperties naverOAuthProperties;
    @Mock
    private SocialAccountRepository socialAccountRepository;
    @Mock
    private GuestMigrationService guestMigrationService;
    @Mock
    private PendingRegistrationService pendingRegistrationService;
    @Mock
    private RestClient naverRestClient;
    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec uriSpec;
    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec headersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private NaverOAuthService naverOAuthService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(naverOAuthProperties.getClientId()).thenReturn("test-client-id");
        lenient().when(naverOAuthProperties.getRedirectUri()).thenReturn("https://example.com/callback");
        lenient().when(naverRestClient.get()).thenReturn(uriSpec);
        lenient().when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        lenient().when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("generateAuthorizationUrl - state를 Redis에 저장하고 네이버 인증 URL을 반환한다")
    void generateAuthorizationUrl_정상() {
        String url = naverOAuthService.generateAuthorizationUrl();

        assertThat(url).contains("nid.naver.com/oauth2.0/authorize");
        assertThat(url).contains("client_id=test-client-id");
        verify(valueOps).set(anyString(), eq("1"), eq(Duration.ofSeconds(600)));
    }

    @Nested
    @DisplayName("processCallback")
    class ProcessCallback {

        private static final String VALID_STATE = "valid-state-uuid";
        private static final String STATE_KEY = "naver:oauth:state:" + VALID_STATE;

        @SuppressWarnings("unchecked")
        @BeforeEach
        void setUpCallback() {
            given(valueOps.getAndDelete(STATE_KEY)).willReturn("1");
            given(responseSpec.body(NaverTokenResponse.class))
                .willReturn(buildTokenResponse("access-token-abc"));
            given(responseSpec.body(NaverProfileResponse.class))
                .willReturn(buildProfileResponse("naver-123", "user@naver.com"));
        }

        @Test
        @DisplayName("기존 회원이면 isNewUser=false와 userId를 반환한다")
        void processCallback_기존회원() {
            User user = mockUser(10L);
            SocialAccount account = mockSocialAccount(user);
            given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.NAVER, "naver-123"))
                .willReturn(Optional.of(account));

            NaverLoginResult result = naverOAuthService.processCallback("code", VALID_STATE, null);

            assertThat(result.isNewUser()).isFalse();
            assertThat(result.userId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("신규 회원이면 isNewUser=true와 tempToken을 반환한다")
        void processCallback_신규회원() {
            given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.NAVER, "naver-123"))
                .willReturn(Optional.empty());
            given(pendingRegistrationService.create(eq(OAuthProvider.NAVER), anyString(), anyString()))
                .willReturn("temp-token-xyz");

            NaverLoginResult result = naverOAuthService.processCallback("code", VALID_STATE, null);

            assertThat(result.isNewUser()).isTrue();
            assertThat(result.tempToken()).isEqualTo("temp-token-xyz");
        }

        @Test
        @DisplayName("기존 회원 + guestId가 있으면 Guest 마이그레이션을 실행한다")
        void processCallback_기존회원_guestId_마이그레이션() {
            User user = mockUser(10L);
            SocialAccount account = mockSocialAccount(user);
            given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.NAVER, "naver-123"))
                .willReturn(Optional.of(account));
            given(guestMigrationService.migrateGuestData("guest-uuid", user))
                .willReturn(new GuestMigrationService.MigrationResult(true, 0));

            naverOAuthService.processCallback("code", VALID_STATE, "guest-uuid");

            verify(guestMigrationService).migrateGuestData("guest-uuid", user);
        }

        @Test
        @DisplayName("state가 유효하지 않으면 BadRequestException을 던진다")
        void processCallback_state_유효하지않음() {
            given(valueOps.getAndDelete("naver:oauth:state:invalid-state")).willReturn(null);

            assertThatThrownBy(() ->
                naverOAuthService.processCallback("code", "invalid-state", null))
                .isInstanceOf(BadRequestException.class);
        }
    }

    private NaverTokenResponse buildTokenResponse(String accessToken) {
        NaverTokenResponse response = new NaverTokenResponse();
        ReflectionTestUtils.setField(response, "accessToken", accessToken);
        return response;
    }

    private NaverProfileResponse buildProfileResponse(String naverId, String email) {
        NaverProfileResponse.NaverUserDetail detail = new NaverProfileResponse.NaverUserDetail();
        ReflectionTestUtils.setField(detail, "id", naverId);
        ReflectionTestUtils.setField(detail, "email", email);

        NaverProfileResponse profile = new NaverProfileResponse();
        ReflectionTestUtils.setField(profile, "resultcode", "00");
        ReflectionTestUtils.setField(profile, "response", detail);
        return profile;
    }

    private User mockUser(Long id) {
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    private SocialAccount mockSocialAccount(User user) {
        SocialAccount account = org.mockito.Mockito.mock(SocialAccount.class);
        given(account.getUser()).willReturn(user);
        return account;
    }
}

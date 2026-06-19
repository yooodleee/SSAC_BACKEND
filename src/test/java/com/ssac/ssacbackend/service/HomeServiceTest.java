package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.response.HomeResponse;
import com.ssac.ssacbackend.dto.response.OnboardingRequiredResponse;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.HomeContentAssembler.ContentSections;
import java.time.LocalDateTime;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomeServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private HomeContentAssembler homeContentAssembler;
    @Mock
    private HomeQuizAssembler homeQuizAssembler;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private HomeService homeService;

    private static final ObjectMapper TEST_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(homeContentAssembler.build(anyLong(), anyString(), any(), anySet()))
            .thenReturn(defaultContentSections());
        lenient().when(homeQuizAssembler.build(anyLong(), any(), anyString()))
            .thenReturn(null);
    }

    @Nested
    @DisplayName("온보딩 미완료 사용자")
    class OnboardingIncomplete {

        @Test
        @DisplayName("onboardingRequired: true 응답")
        void 온보딩_미완료_사용자_onboardingRequired_true() {
            User user = mockUser(1L, false, UserLevel.SEED, null);
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

            Object result = homeService.getHome("test@test.com");

            assertThat(result).isInstanceOf(OnboardingRequiredResponse.class);
            OnboardingRequiredResponse res = (OnboardingRequiredResponse) result;
            assertThat(res.onboardingRequired()).isTrue();
            assertThat(res.redirectTo()).isEqualTo("/onboarding/test");
        }
    }

    @Nested
    @DisplayName("온보딩 완료 사용자 - 캐시 히트")
    class CacheHit {

        @Test
        @DisplayName("캐시 히트 시 어셈블러 호출 없이 캐시 데이터 반환")
        void 캐시_히트_시_어셈블러_호출_없음() throws Exception {
            User user = mockUser(1L, true, UserLevel.SPROUT, null);
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
            HomeResponse cached = mockHomeResponse();
            given(valueOps.get("home:1")).willReturn(TEST_MAPPER.writeValueAsString(cached));

            Object result = homeService.getHome("test@test.com");

            assertThat(result).isInstanceOf(HomeResponse.class);
            assertThat(((HomeResponse) result).user().nickname()).isEqualTo("닉네임");
            then(homeContentAssembler).should(never()).build(anyLong(), anyString(), any(), anySet());
        }

        @Test
        @DisplayName("캐시 무효화 후 어셈블러를 통해 최신 데이터 응답")
        void 캐시_무효화_후_어셈블러_호출() {
            User user = mockUser(1L, true, UserLevel.SPROUT, null);
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:1")).willReturn(null);
            given(valueOps.get("home:rec_history:1")).willReturn(null);

            Object result = homeService.getHome("test@test.com");

            assertThat(result).isInstanceOf(HomeResponse.class);
            HomeResponse res = (HomeResponse) result;
            assertThat(res.onboardingRequired()).isFalse();
            then(homeContentAssembler).should().build(anyLong(), anyString(), any(), anySet());
        }
    }

    @Nested
    @DisplayName("온보딩 완료 사용자 - 캐시 미스")
    class CacheMiss {

        @Test
        @DisplayName("온보딩 완료 사용자 홈 데이터 응답 확인")
        void 온보딩_완료_사용자_홈_데이터_응답() {
            User user = mockUser(2L, true, UserLevel.SPROUT, null);
            given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:2")).willReturn(null);
            given(valueOps.get("home:rec_history:2")).willReturn(null);

            Object result = homeService.getHome("user@test.com");

            assertThat(result).isInstanceOf(HomeResponse.class);
            HomeResponse res = (HomeResponse) result;
            assertThat(res.onboardingRequired()).isFalse();
            assertThat(res.user().level()).isEqualTo("SPROUT");
        }

        @Test
        @DisplayName("7일 이상 미접속 시 welcomeBack 포함 확인")
        void 장기_미접속_welcomeBack_포함() {
            LocalDateTime oldVisit = LocalDateTime.now().minusDays(10);
            User user = mockUser(5L, true, UserLevel.SPROUT, oldVisit);
            given(userRepository.findByEmail("test5@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:5")).willReturn(null);
            given(valueOps.get("home:rec_history:5")).willReturn(null);

            Object result = homeService.getHome("test5@test.com");

            HomeResponse res = (HomeResponse) result;
            assertThat(res.welcomeBack()).isNotNull();
            assertThat(res.welcomeBack().isLongAbsence()).isTrue();
            assertThat(res.welcomeBack().daysSinceLastVisit()).isGreaterThanOrEqualTo(7);
        }

        @Test
        @DisplayName("7일 미만 접속 시 welcomeBack은 null이다")
        void 최근_접속_welcomeBack_null() {
            LocalDateTime recentVisit = LocalDateTime.now().minusDays(3);
            User user = mockUser(6L, true, UserLevel.SEED, recentVisit);
            given(userRepository.findByEmail("test6@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:6")).willReturn(null);
            given(valueOps.get("home:rec_history:6")).willReturn(null);

            Object result = homeService.getHome("test6@test.com");

            HomeResponse res = (HomeResponse) result;
            assertThat(res.welcomeBack()).isNull();
        }

        @Test
        @DisplayName("첫 접속(lastVisitedAt null)이면 welcomeBack은 null이다")
        void 첫_접속_welcomeBack_null() {
            User user = mockUser(7L, true, UserLevel.SEED, null);
            given(userRepository.findByEmail("test7@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:7")).willReturn(null);
            given(valueOps.get("home:rec_history:7")).willReturn(null);

            Object result = homeService.getHome("test7@test.com");

            HomeResponse res = (HomeResponse) result;
            assertThat(res.welcomeBack()).isNull();
        }
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private User mockUser(Long id, boolean onboardingCompleted, UserLevel level,
                          LocalDateTime lastVisitedAt) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        given(user.getEmail()).willReturn("test@test.com");
        given(user.getRole()).willReturn(UserRole.USER);
        given(user.getNickname()).willReturn("닉네임");
        given(user.isOnboardingCompleted()).willReturn(onboardingCompleted);
        given(user.getLevel()).willReturn(level);
        given(user.getLastVisitedAt()).willReturn(lastVisitedAt);
        return user;
    }

    private ContentSections defaultContentSections() {
        return new ContentSections(List.of(), null, null, List.of());
    }

    private HomeResponse mockHomeResponse() {
        return new HomeResponse(
            false,
            new HomeResponse.HomeUserDto("닉네임", "HIGH_SCHOOL", "SPROUT", "새싹", "🌱", "sprout"),
            null, List.of(), null, null, List.of(), null, null
        );
    }
}

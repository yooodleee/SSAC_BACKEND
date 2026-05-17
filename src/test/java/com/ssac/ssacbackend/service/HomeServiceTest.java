package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.response.HomeResponse;
import com.ssac.ssacbackend.dto.response.OnboardingRequiredResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.QuizRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.repository.UserRepository;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomeServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserInterestRepository userInterestRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ContentProgressRepository contentProgressRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private HomeService homeService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
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
        @DisplayName("캐시 히트 시 DB 조회 없이 캐시 데이터 반환")
        void 캐시_히트_시_DB_조회_없음() {
            User user = mockUser(1L, true, UserLevel.SPROUT, null);
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
            HomeResponse cached = mockHomeResponse();
            given(valueOps.get("home:1")).willReturn(cached);

            Object result = homeService.getHome("test@test.com");

            assertThat(result).isSameAs(cached);
            then(contentRepository).should(never()).findByDifficultyOrderByViewCountDesc(any());
        }

        @Test
        @DisplayName("캐시 무효화 후 최신 데이터 응답")
        void 캐시_무효화_후_최신_데이터_응답() {
            User user = mockUser(1L, true, UserLevel.SPROUT, null);
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:1")).willReturn(null); // 캐시 미스

            given(userInterestRepository.findDomainIdsByUserId(1L)).willReturn(List.of());
            given(contentProgressRepository.findCompletedContentIdsByUserEmail(anyString()))
                .willReturn(List.of());
            given(valueOps.get("home:rec_history:1")).willReturn(null);
            given(contentRepository.findByDifficultyOrderByViewCountDesc(any())).willReturn(List.of());
            given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of());
            given(contentProgressRepository.findContinueLearning(anyString(), any()))
                .willReturn(List.of());
            given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
                .willReturn(List.of());

            Object result = homeService.getHome("test@test.com");

            assertThat(result).isInstanceOf(HomeResponse.class);
            HomeResponse res = (HomeResponse) result;
            assertThat(res.onboardingRequired()).isFalse();
        }
    }

    @Nested
    @DisplayName("온보딩 완료 사용자 - 캐시 미스")
    class CacheMiss {

        @Test
        @DisplayName("온보딩 완료 사용자 맞춤 홈 데이터 응답 확인")
        void 온보딩_완료_사용자_맞춤_홈_데이터_응답() {
            User user = mockUser(2L, true, UserLevel.SPROUT, null);
            given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:2")).willReturn(null);

            given(userInterestRepository.findDomainIdsByUserId(2L)).willReturn(List.of("finance"));
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("user@test.com"))
                .willReturn(List.of());
            given(valueOps.get("home:rec_history:2")).willReturn(null);

            Content content = mockContent(10L, "투자 기초", "finance", UserLevel.SPROUT);
            given(contentRepository.findByCategoryInAndDifficultyOrderByViewCountDesc(
                eq(List.of("finance")), eq(UserLevel.SPROUT)))
                .willReturn(List.of(content));
            given(contentRepository.findByDifficultyOrderByViewCountDesc(UserLevel.SPROUT))
                .willReturn(List.of());
            given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of());
            given(contentProgressRepository.findContinueLearning(anyString(), any()))
                .willReturn(List.of());
            given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
                .willReturn(List.of());

            Object result = homeService.getHome("user@test.com");

            assertThat(result).isInstanceOf(HomeResponse.class);
            HomeResponse res = (HomeResponse) result;
            assertThat(res.onboardingRequired()).isFalse();
            assertThat(res.user().level()).isEqualTo("SPROUT");
        }

        @Test
        @DisplayName("콘텐츠 완료 후 해당 콘텐츠 추천 목록 제외 확인")
        void 완료된_콘텐츠_추천_제외() {
            User user = mockUser(3L, true, UserLevel.SEED, null);
            given(userRepository.findByEmail("test3@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:3")).willReturn(null);
            given(userInterestRepository.findDomainIdsByUserId(3L)).willReturn(List.of());
            given(valueOps.get("home:rec_history:3")).willReturn(null);

            // 완료된 콘텐츠 ID: 100
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("test3@test.com"))
                .willReturn(List.of(100L));

            Content completedContent = mockContent(100L, "완료된 콘텐츠", "finance", UserLevel.SEED);
            Content newContent = mockContent(200L, "새 콘텐츠", "finance", UserLevel.SEED);
            given(contentRepository.findByDifficultyOrderByViewCountDesc(UserLevel.SEED))
                .willReturn(List.of(completedContent, newContent));
            given(contentRepository.findAllByOrderByViewCountDesc())
                .willReturn(List.of(completedContent, newContent));
            given(contentProgressRepository.findContinueLearning(anyString(), any()))
                .willReturn(List.of());
            given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
                .willReturn(List.of());

            Object result = homeService.getHome("test3@test.com");

            HomeResponse res = (HomeResponse) result;
            List<String> recommendedIds = res.recommendedContents().stream()
                .map(HomeResponse.RecommendedContentDto::id)
                .toList();
            assertThat(recommendedIds).doesNotContain("100");
            assertThat(recommendedIds).contains("200");
        }

        @Test
        @DisplayName("추천 콘텐츠 소진 시 상위 레벨 미리보기 응답")
        void 추천_소진_시_상위_레벨_미리보기() {
            User user = mockUser(4L, true, UserLevel.SEED, null);
            given(userRepository.findByEmail("test4@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:4")).willReturn(null);
            given(userInterestRepository.findDomainIdsByUserId(4L)).willReturn(List.of());
            given(valueOps.get("home:rec_history:4")).willReturn(null);
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("test4@test.com"))
                .willReturn(List.of());

            // SEED 레벨 콘텐츠 없음
            given(contentRepository.findByDifficultyOrderByViewCountDesc(UserLevel.SEED))
                .willReturn(List.of());
            given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of());

            // SPROUT 레벨 미리보기 콘텐츠
            Content previewContent = mockContent(500L, "SPROUT 미리보기", "finance", UserLevel.SPROUT);
            given(contentRepository.findByDifficultyOrderByViewCountDesc(UserLevel.SPROUT))
                .willReturn(List.of(previewContent));
            given(contentProgressRepository.findContinueLearning(anyString(), any()))
                .willReturn(List.of());
            given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
                .willReturn(List.of());

            Object result = homeService.getHome("test4@test.com");

            HomeResponse res = (HomeResponse) result;
            assertThat(res.recommendedContents())
                .anyMatch(HomeResponse.RecommendedContentDto::isPreview);
        }

        @Test
        @DisplayName("7일 이상 미접속 시 welcomeBack 포함 확인")
        void 장기_미접속_welcomeBack_포함() {
            LocalDateTime oldVisit = LocalDateTime.now().minusDays(10);
            User user = mockUser(5L, true, UserLevel.SPROUT, oldVisit);
            given(userRepository.findByEmail("test5@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:5")).willReturn(null);
            given(userInterestRepository.findDomainIdsByUserId(5L)).willReturn(List.of());
            given(valueOps.get("home:rec_history:5")).willReturn(null);
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("test5@test.com"))
                .willReturn(List.of());
            given(contentRepository.findByDifficultyOrderByViewCountDesc(any())).willReturn(List.of());
            given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of());
            given(contentProgressRepository.findContinueLearning(anyString(), any()))
                .willReturn(List.of());
            given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
                .willReturn(List.of());

            Object result = homeService.getHome("test5@test.com");

            HomeResponse res = (HomeResponse) result;
            assertThat(res.welcomeBack()).isNotNull();
            assertThat(res.welcomeBack().isLongAbsence()).isTrue();
            assertThat(res.welcomeBack().daysSinceLastVisit()).isGreaterThanOrEqualTo(7);
        }

        @Test
        @DisplayName("레벨 변경 후 새 레벨 기반 추천 콘텐츠 응답 확인")
        void 레벨_변경_후_새_레벨_기반_추천() {
            User user = mockUser(6L, true, UserLevel.TREE, null);
            given(userRepository.findByEmail("test6@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:6")).willReturn(null);
            given(userInterestRepository.findDomainIdsByUserId(6L)).willReturn(List.of());
            given(valueOps.get("home:rec_history:6")).willReturn(null);
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("test6@test.com"))
                .willReturn(List.of());

            Content treeContent = mockContent(700L, "TREE 콘텐츠", "finance", UserLevel.TREE);
            given(contentRepository.findByDifficultyOrderByViewCountDesc(UserLevel.TREE))
                .willReturn(List.of(treeContent));
            given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of());
            given(contentProgressRepository.findContinueLearning(anyString(), any()))
                .willReturn(List.of());
            given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
                .willReturn(List.of());

            Object result = homeService.getHome("test6@test.com");

            HomeResponse res = (HomeResponse) result;
            assertThat(res.user().level()).isEqualTo("TREE");
            assertThat(res.recommendedContents()).anyMatch(c -> c.id().equals("700"));
        }

        @Test
        @DisplayName("관심 도메인 변경 후 추천 콘텐츠 재구성 확인")
        void 관심_도메인_변경_후_추천_재구성() {
            User user = mockUser(7L, true, UserLevel.SEED, null);
            given(userRepository.findByEmail("test7@test.com")).willReturn(Optional.of(user));
            given(valueOps.get("home:7")).willReturn(null);

            // 변경된 관심 도메인: resume
            given(userInterestRepository.findDomainIdsByUserId(7L)).willReturn(List.of("resume"));
            given(valueOps.get("home:rec_history:7")).willReturn(null);
            given(contentProgressRepository.findCompletedContentIdsByUserEmail("test7@test.com"))
                .willReturn(List.of());

            Content resumeContent = mockContent(800L, "이력서 작성", "resume", UserLevel.SEED);
            given(contentRepository.findByCategoryInAndDifficultyOrderByViewCountDesc(
                eq(List.of("resume")), eq(UserLevel.SEED)))
                .willReturn(List.of(resumeContent));
            given(contentRepository.findByDifficultyOrderByViewCountDesc(UserLevel.SEED))
                .willReturn(List.of());
            given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of());
            given(contentProgressRepository.findContinueLearning(anyString(), any()))
                .willReturn(List.of());
            given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
                .willReturn(List.of());

            Object result = homeService.getHome("test7@test.com");

            HomeResponse res = (HomeResponse) result;
            assertThat(res.recommendedContents()).anyMatch(c -> c.id().equals("800"));
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

    private Content mockContent(Long id, String title, String category, UserLevel difficulty) {
        Content content = mock(Content.class);
        given(content.getId()).willReturn(id);
        given(content.getTitle()).willReturn(title);
        given(content.getCategory()).willReturn(category);
        given(content.getDifficulty()).willReturn(difficulty);
        given(content.getEstimatedMinutes()).willReturn(5);
        return content;
    }

    private HomeResponse mockHomeResponse() {
        return new HomeResponse(
            false,
            new HomeResponse.HomeUserDto("닉네임", "HIGH_SCHOOL", "SPROUT", "새싹", "🌱"),
            null, List.of(), null, null, List.of(), null, null
        );
    }
}

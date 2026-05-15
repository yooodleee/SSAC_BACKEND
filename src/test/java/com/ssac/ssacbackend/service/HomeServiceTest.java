package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentProgress;
import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.response.HomeResponse;
import com.ssac.ssacbackend.dto.response.OnboardingRequiredResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.QuizRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserInterestRepository userInterestRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private ContentProgressRepository contentProgressRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private QuizAttemptRepository quizAttemptRepository;

    @InjectMocks
    private HomeService homeService;

    // ── 온보딩 미완료 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("온보딩 미완료 사용자 요청 시 onboardingRequired 응답")
    void 온보딩_미완료_사용자_요청() {
        User user = buildUser(1L, "nick", UserType.HIGH_SCHOOL, UserLevel.SEED, false);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        Object result = homeService.getHome("test@test.com");

        assertThat(result).isInstanceOf(OnboardingRequiredResponse.class);
        OnboardingRequiredResponse resp = (OnboardingRequiredResponse) result;
        assertThat(resp.onboardingRequired()).isTrue();
        assertThat(resp.redirectTo()).isEqualTo("/onboarding/test");
    }

    // ── HIGH_SCHOOL + SEED ────────────────────────────────────────────────────

    @Test
    @DisplayName("HIGH_SCHOOL + SEED 사용자 홈 API 응답 구조 검증")
    void HIGH_SCHOOL_SEED_홈_응답_구조_검증() {
        User user = buildUser(1L, "새싹이", UserType.HIGH_SCHOOL, UserLevel.SEED, true);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userInterestRepository.findDomainIdsByUserId(1L)).willReturn(List.of("realestate"));
        given(contentProgressRepository.findCompletedContentIdsByUserEmail("test@test.com"))
            .willReturn(List.of());

        Content content = buildContent(1L, "전세 계약 기초", "realestate", UserLevel.SEED, 5, 100L);
        given(contentRepository.findByCategoryInAndDifficultyOrderByViewCountDesc(
            List.of("realestate"), UserLevel.SEED)).willReturn(List.of(content));
        given(contentRepository.findByDifficultyOrderByViewCountDesc(UserLevel.SEED))
            .willReturn(List.of(content));
        given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of(content));

        given(contentProgressRepository.findContinueLearning(eq("test@test.com"), any(Pageable.class)))
            .willReturn(List.of());
        given(quizRepository.findUncompletedByDifficultyAndUserEmail(UserLevel.SEED, "test@test.com"))
            .willReturn(List.of());

        given(contentRepository.countByCategory(anyString())).willReturn(10L);
        given(contentProgressRepository.countCompletedByUserEmailAndCategory(anyString(), anyString()))
            .willReturn(2L);

        Object result = homeService.getHome("test@test.com");

        assertThat(result).isInstanceOf(HomeResponse.class);
        HomeResponse resp = (HomeResponse) result;
        assertThat(resp.user().userType()).isEqualTo("HIGH_SCHOOL");
        assertThat(resp.user().level()).isEqualTo("SEED");
        assertThat(resp.user().levelLabel()).isEqualTo("씨앗");
        assertThat(resp.user().levelEmoji()).isEqualTo("🌱");
        assertThat(resp.recommendedContents()).isNotNull();
        assertThat(resp.categories()).hasSize(4);
        assertThat(resp.todayQuiz()).isNull();
    }

    // ── EARLY_CAREER + TREE ────────────────────────────────────────────────────

    @Test
    @DisplayName("EARLY_CAREER + TREE 사용자 홈 API 응답 구조 검증")
    void EARLY_CAREER_TREE_홈_응답_구조_검증() {
        User user = buildUser(2L, "나무님", UserType.EARLY_CAREER, UserLevel.TREE, true);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userInterestRepository.findDomainIdsByUserId(2L)).willReturn(List.of("tax", "finance"));
        given(contentProgressRepository.findCompletedContentIdsByUserEmail("test@test.com"))
            .willReturn(List.of());

        Content treeContent = buildContent(10L, "연말정산 심화", "tax", UserLevel.TREE, 8, 500L);
        given(contentRepository.findByCategoryInAndDifficultyOrderByViewCountDesc(
            List.of("tax", "finance"), UserLevel.TREE)).willReturn(List.of(treeContent));
        given(contentRepository.findByDifficultyOrderByViewCountDesc(UserLevel.TREE))
            .willReturn(List.of(treeContent));
        given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of(treeContent));

        given(contentProgressRepository.findContinueLearning(eq("test@test.com"), any(Pageable.class)))
            .willReturn(List.of());
        given(quizRepository.findUncompletedByDifficultyAndUserEmail(UserLevel.TREE, "test@test.com"))
            .willReturn(List.of());

        given(contentRepository.countByCategory(anyString())).willReturn(20L);
        given(contentProgressRepository.countCompletedByUserEmailAndCategory(anyString(), anyString()))
            .willReturn(5L);

        Object result = homeService.getHome("test@test.com");

        assertThat(result).isInstanceOf(HomeResponse.class);
        HomeResponse resp = (HomeResponse) result;
        assertThat(resp.user().userType()).isEqualTo("EARLY_CAREER");
        assertThat(resp.user().level()).isEqualTo("TREE");
        assertThat(resp.user().levelLabel()).isEqualTo("나무");
        assertThat(resp.user().levelEmoji()).isEqualTo("🌳");
    }

    // ── 추천 콘텐츠 우선순위 ───────────────────────────────────────────────────

    @Test
    @DisplayName("관심 도메인 기반 추천 콘텐츠 우선순위 검증")
    void 관심_도메인_기반_추천_콘텐츠_우선순위_검증() {
        User user = buildUser(1L, "nick", UserType.HIGH_SCHOOL, UserLevel.SEED, true);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userInterestRepository.findDomainIdsByUserId(1L)).willReturn(List.of("realestate"));
        given(contentProgressRepository.findCompletedContentIdsByUserEmail("test@test.com"))
            .willReturn(List.of());

        Content interestContent = buildContent(1L, "관심 도메인 콘텐츠", "realestate", UserLevel.SEED, 3, 100L);
        Content levelContent = buildContent(2L, "레벨 일치 콘텐츠", "tax", UserLevel.SEED, 5, 50L);
        given(contentRepository.findByCategoryInAndDifficultyOrderByViewCountDesc(
            List.of("realestate"), UserLevel.SEED)).willReturn(List.of(interestContent));
        given(contentRepository.findByDifficultyOrderByViewCountDesc(UserLevel.SEED))
            .willReturn(List.of(interestContent, levelContent));
        given(contentRepository.findAllByOrderByViewCountDesc())
            .willReturn(List.of(interestContent, levelContent));
        given(contentProgressRepository.findContinueLearning(eq("test@test.com"), any(Pageable.class)))
            .willReturn(List.of());
        given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
            .willReturn(List.of());
        given(contentRepository.countByCategory(anyString())).willReturn(5L);
        given(contentProgressRepository.countCompletedByUserEmailAndCategory(anyString(), anyString()))
            .willReturn(0L);

        HomeResponse resp = (HomeResponse) homeService.getHome("test@test.com");

        // 관심 도메인 콘텐츠가 첫 번째로 와야 함
        assertThat(resp.recommendedContents()).isNotEmpty();
        assertThat(resp.recommendedContents().get(0).id()).isEqualTo("1");
        assertThat(resp.recommendedContents().get(0).category()).isEqualTo("realestate");
    }

    // ── 레벨 기반 필터링 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("레벨 기반 콘텐츠 필터링 정확성 검증")
    void 레벨_기반_콘텐츠_필터링_검증() {
        User user = buildUser(1L, "nick", UserType.EARLY_CAREER, UserLevel.SPROUT, true);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userInterestRepository.findDomainIdsByUserId(1L)).willReturn(List.of());
        given(contentProgressRepository.findCompletedContentIdsByUserEmail("test@test.com"))
            .willReturn(List.of());

        Content sproutContent = buildContent(5L, "초보 콘텐츠", "finance", UserLevel.SPROUT, 4, 200L);
        given(contentRepository.findByDifficultyOrderByViewCountDesc(UserLevel.SPROUT))
            .willReturn(List.of(sproutContent));
        given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of(sproutContent));
        given(contentProgressRepository.findContinueLearning(eq("test@test.com"), any(Pageable.class)))
            .willReturn(List.of());
        given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
            .willReturn(List.of());
        given(contentRepository.countByCategory(anyString())).willReturn(5L);
        given(contentProgressRepository.countCompletedByUserEmailAndCategory(anyString(), anyString()))
            .willReturn(0L);

        HomeResponse resp = (HomeResponse) homeService.getHome("test@test.com");

        assertThat(resp.recommendedContents()).hasSize(1);
        assertThat(resp.recommendedContents().get(0).difficultyLabel()).isEqualTo("초보");
    }

    // ── todayCard 당일 동일 응답 ─────────────────────────────────────────────

    @Test
    @DisplayName("todayCard 당일 동일 응답 검증")
    void todayCard_당일_동일_응답_검증() {
        User user = buildUser(1L, "nick", UserType.HIGH_SCHOOL, UserLevel.SEED, true);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userInterestRepository.findDomainIdsByUserId(1L)).willReturn(List.of("realestate"));
        given(contentProgressRepository.findCompletedContentIdsByUserEmail("test@test.com"))
            .willReturn(List.of());

        Content c1 = buildContent(1L, "콘텐츠A", "realestate", UserLevel.SEED, 3, 100L);
        Content c2 = buildContent(2L, "콘텐츠B", "realestate", UserLevel.SEED, 5, 80L);
        given(contentRepository.findByCategoryInAndDifficultyOrderByViewCountDesc(any(), any()))
            .willReturn(List.of(c1, c2));
        given(contentRepository.findByDifficultyOrderByViewCountDesc(any())).willReturn(List.of(c1, c2));
        given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of(c1, c2));
        given(contentProgressRepository.findContinueLearning(eq("test@test.com"), any(Pageable.class)))
            .willReturn(List.of());
        given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
            .willReturn(List.of());
        given(contentRepository.countByCategory(anyString())).willReturn(5L);
        given(contentProgressRepository.countCompletedByUserEmailAndCategory(anyString(), anyString()))
            .willReturn(0L);

        HomeResponse resp1 = (HomeResponse) homeService.getHome("test@test.com");
        HomeResponse resp2 = (HomeResponse) homeService.getHome("test@test.com");

        // 동일 호출에서 todayCard는 동일해야 함
        assertThat(resp1.todayCard().id()).isEqualTo(resp2.todayCard().id());
    }

    // ── continueLearning ──────────────────────────────────────────────────────

    @Test
    @DisplayName("continueLearning 미완료 콘텐츠 조회 검증")
    void continueLearning_미완료_콘텐츠_조회_검증() {
        User user = buildUser(1L, "nick", UserType.HIGH_SCHOOL, UserLevel.SEED, true);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userInterestRepository.findDomainIdsByUserId(1L)).willReturn(List.of());
        given(contentProgressRepository.findCompletedContentIdsByUserEmail("test@test.com"))
            .willReturn(List.of());
        given(contentRepository.findByDifficultyOrderByViewCountDesc(any())).willReturn(List.of());
        given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of());
        given(contentRepository.countByCategory(anyString())).willReturn(0L);
        given(contentProgressRepository.countCompletedByUserEmailAndCategory(anyString(), anyString()))
            .willReturn(0L);
        given(quizRepository.findUncompletedByDifficultyAndUserEmail(any(), anyString()))
            .willReturn(List.of());

        ContentProgress inProgress = buildContentProgress(42L, "진행중 콘텐츠", "tax", 60);
        given(contentProgressRepository.findContinueLearning(eq("test@test.com"), any(Pageable.class)))
            .willReturn(List.of(inProgress));

        HomeResponse resp = (HomeResponse) homeService.getHome("test@test.com");

        assertThat(resp.continueLearning()).isNotNull();
        assertThat(resp.continueLearning().id()).isEqualTo("42");
        assertThat(resp.continueLearning().title()).isEqualTo("진행중 콘텐츠");
        assertThat(resp.continueLearning().category()).isEqualTo("tax");
        assertThat(resp.continueLearning().progressRate()).isEqualTo(60);
    }

    // ── todayQuiz null ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("모든 퀴즈 완료 시 todayQuiz null 응답 검증")
    void 모든_퀴즈_완료_시_todayQuiz_null_응답() {
        User user = buildUser(1L, "nick", UserType.EARLY_CAREER, UserLevel.TREE, true);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userInterestRepository.findDomainIdsByUserId(1L)).willReturn(List.of());
        given(contentProgressRepository.findCompletedContentIdsByUserEmail("test@test.com"))
            .willReturn(List.of());
        given(contentRepository.findByDifficultyOrderByViewCountDesc(any())).willReturn(List.of());
        given(contentRepository.findAllByOrderByViewCountDesc()).willReturn(List.of());
        given(contentProgressRepository.findContinueLearning(eq("test@test.com"), any(Pageable.class)))
            .willReturn(List.of());
        given(contentRepository.countByCategory(anyString())).willReturn(0L);
        given(contentProgressRepository.countCompletedByUserEmailAndCategory(anyString(), anyString()))
            .willReturn(0L);

        // 모든 퀴즈 완료 → 빈 목록 반환
        given(quizRepository.findUncompletedByDifficultyAndUserEmail(UserLevel.TREE, "test@test.com"))
            .willReturn(List.of());

        HomeResponse resp = (HomeResponse) homeService.getHome("test@test.com");

        assertThat(resp.todayQuiz()).isNull();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private User buildUser(Long id, String nickname, UserType userType,
                           UserLevel level, boolean onboardingCompleted) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getNickname()).thenReturn(nickname);
        lenient().when(user.getUserType()).thenReturn(userType);
        lenient().when(user.getLevel()).thenReturn(level);
        lenient().when(user.isOnboardingCompleted()).thenReturn(onboardingCompleted);
        return user;
    }

    private Content buildContent(Long id, String title, String category,
                                  UserLevel difficulty, int minutes, long viewCount) {
        Content content = mock(Content.class);
        lenient().when(content.getId()).thenReturn(id);
        lenient().when(content.getTitle()).thenReturn(title);
        lenient().when(content.getCategory()).thenReturn(category);
        lenient().when(content.getDifficulty()).thenReturn(difficulty);
        lenient().when(content.getEstimatedMinutes()).thenReturn(minutes);
        lenient().when(content.getViewCount()).thenReturn(viewCount);
        return content;
    }

    private ContentProgress buildContentProgress(Long id, String title, String category, int progress) {
        ContentProgress cp = mock(ContentProgress.class);
        lenient().when(cp.getId()).thenReturn(id);
        lenient().when(cp.getTitle()).thenReturn(title);
        lenient().when(cp.getCategory()).thenReturn(category);
        lenient().when(cp.getProgressRate()).thenReturn(progress);
        return cp;
    }
}

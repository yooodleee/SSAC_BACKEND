package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.domain.onboarding.OnboardingQuestion;
import com.ssac.ssacbackend.domain.onboarding.UserInterest;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.request.OnboardingInterestsRequest;
import com.ssac.ssacbackend.dto.request.OnboardingSubmitRequest;
import com.ssac.ssacbackend.dto.response.OnboardingQuestionsResponse;
import com.ssac.ssacbackend.dto.response.OnboardingResultResponse;
import com.ssac.ssacbackend.dto.response.OnboardingSkipResponse;
import com.ssac.ssacbackend.dto.response.OnboardingSubmitResponse;
import com.ssac.ssacbackend.repository.OnboardingQuestionRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OnboardingQuestionRepository onboardingQuestionRepository;

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private HomeCacheEvictService homeCacheEvictService;

    @InjectMocks
    private OnboardingService onboardingService;

    // ── getQuestions ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("HIGH_SCHOOL 유형 문제 5개 조회 성공")
    void HIGH_SCHOOL_유형_문제_조회_성공() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, false, false, UserLevel.SEED, 0);
        List<OnboardingQuestion> questions = buildQuestions(UserType.HIGH_SCHOOL, 5);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(onboardingQuestionRepository
            .findByUserTypeAndIsActiveTrueOrderByQuestionOrderAsc(UserType.HIGH_SCHOOL))
            .willReturn(questions);

        OnboardingQuestionsResponse response = onboardingService.getQuestions("test@test.com");

        assertThat(response.userType()).isEqualTo(UserType.HIGH_SCHOOL);
        assertThat(response.totalCount()).isEqualTo(5);
        assertThat(response.questions()).hasSize(5);
        assertThat(response.questions().get(0).options()).hasSize(3);
    }

    @Test
    @DisplayName("EARLY_CAREER 유형 문제 5개 조회 성공")
    void EARLY_CAREER_유형_문제_조회_성공() {
        User user = buildUser("test@test.com", UserType.EARLY_CAREER, false, false, UserLevel.SEED, 0);
        List<OnboardingQuestion> questions = buildQuestions(UserType.EARLY_CAREER, 5);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(onboardingQuestionRepository
            .findByUserTypeAndIsActiveTrueOrderByQuestionOrderAsc(UserType.EARLY_CAREER))
            .willReturn(questions);

        OnboardingQuestionsResponse response = onboardingService.getQuestions("test@test.com");

        assertThat(response.userType()).isEqualTo(UserType.EARLY_CAREER);
        assertThat(response.totalCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("userType null 상태 요청 시 400(ONBOARDING-001) 응답")
    void userType_null_요청_시_400_응답() {
        User user = buildUser("test@test.com", null, false, false, UserLevel.SEED, 0);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> onboardingService.getQuestions("test@test.com"))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(be.getCode()).isEqualTo("ONBOARDING-001");
            });
    }

    @Test
    @DisplayName("이미 완료한 사용자 요청 시 409(ONBOARDING-002) 응답")
    void 이미_완료한_사용자_요청_시_409_응답() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, true, false, UserLevel.SPROUT, 5);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> onboardingService.getQuestions("test@test.com"))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(be.getCode()).isEqualTo("ONBOARDING-002");
            });
    }

    // ── submit ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("5개 미만 응답 제출 시 400(ONBOARDING-003) 응답")
    void 응답_5개_미만_제출_시_400_응답() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, false, false, UserLevel.SEED, 0);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        OnboardingSubmitRequest request = new OnboardingSubmitRequest(List.of(
            new OnboardingSubmitRequest.Answer(1L, "A"),
            new OnboardingSubmitRequest.Answer(2L, "B")
        ));

        assertThatThrownBy(() -> onboardingService.submit("test@test.com", request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(be.getCode()).isEqualTo("ONBOARDING-003");
            });
    }

    @Test
    @DisplayName("총점 3점 이하 → SEED 판정")
    void 총점_3점_이하_SEED_판정() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, false, false, UserLevel.SEED, 0);
        List<OnboardingQuestion> questions = buildQuestions(UserType.HIGH_SCHOOL, 5);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(onboardingQuestionRepository.findAllById(List.of(1L, 2L, 3L, 4L, 5L)))
            .willReturn(questions);
        // 모두 C 선택 → 총점 0점
        OnboardingSubmitRequest request = buildSubmitRequest(List.of("C", "C", "C", "C", "C"));

        OnboardingSubmitResponse response = onboardingService.submit("test@test.com", request);

        assertThat(response.level()).isEqualTo(UserLevel.SEED);
        assertThat(response.totalScore()).isEqualTo(0);
        assertThat(response.onboardingCompleted()).isTrue();
        verify(user).completeOnboarding(UserLevel.SEED, 0);
    }

    @Test
    @DisplayName("총점 4~7점 → SPROUT 판정")
    void 총점_4점_이상_7점_이하_SPROUT_판정() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, false, false, UserLevel.SEED, 0);
        List<OnboardingQuestion> questions = buildQuestions(UserType.HIGH_SCHOOL, 5);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(onboardingQuestionRepository.findAllById(List.of(1L, 2L, 3L, 4L, 5L)))
            .willReturn(questions);
        // A*2 + C*3 = 4점
        OnboardingSubmitRequest request = buildSubmitRequest(List.of("A", "A", "C", "C", "C"));

        OnboardingSubmitResponse response = onboardingService.submit("test@test.com", request);

        assertThat(response.level()).isEqualTo(UserLevel.SPROUT);
        assertThat(response.totalScore()).isEqualTo(4);
        verify(user).completeOnboarding(UserLevel.SPROUT, 4);
    }

    @Test
    @DisplayName("총점 8점 이상 → TREE 판정")
    void 총점_8점_이상_TREE_판정() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, false, false, UserLevel.SEED, 0);
        List<OnboardingQuestion> questions = buildQuestions(UserType.HIGH_SCHOOL, 5);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(onboardingQuestionRepository.findAllById(List.of(1L, 2L, 3L, 4L, 5L)))
            .willReturn(questions);
        // A*4 + B*1 = 9점
        OnboardingSubmitRequest request = buildSubmitRequest(List.of("A", "A", "A", "A", "B"));

        OnboardingSubmitResponse response = onboardingService.submit("test@test.com", request);

        assertThat(response.level()).isEqualTo(UserLevel.TREE);
        assertThat(response.totalScore()).isEqualTo(9);
        verify(user).completeOnboarding(UserLevel.TREE, 9);
    }

    // ── skip ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("건너뛰기 시 SEED 기본값 설정")
    void 건너뛰기_시_SEED_기본값_설정() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, false, false, UserLevel.SEED, 0);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        OnboardingSkipResponse response = onboardingService.skip("test@test.com");

        assertThat(response.level()).isEqualTo(UserLevel.SEED);
        assertThat(response.onboardingCompleted()).isTrue();
        assertThat(response.skipped()).isTrue();
        verify(user).skipOnboarding();
    }

    // ── getResult ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("온보딩 완료 사용자 결과 조회 성공")
    void 온보딩_완료_사용자_결과_조회_성공() {
        User user = buildUser("test@test.com", UserType.EARLY_CAREER, true, false, UserLevel.SPROUT, 6);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        OnboardingResultResponse response = onboardingService.getResult("test@test.com");

        assertThat(response.level()).isEqualTo("SPROUT");
        assertThat(response.totalScore()).isEqualTo(6);
        assertThat(response.maxScore()).isEqualTo(10);
        assertThat(response.levelLabel()).isEqualTo("새싹");
        assertThat(response.levelEmoji()).isEqualTo("🌿");
        assertThat(response.levelDescription()).isEqualTo("조금은 알고 있어요");
        assertThat(response.skipped()).isFalse();
        assertThat(response.onboardingCompleted()).isTrue();
        assertThat(response.recommendedDomains()).hasSize(3);
    }

    @Test
    @DisplayName("건너뛰기 사용자 결과 조회 성공 (SEED 반환)")
    void 건너뛰기_사용자_결과_조회_성공() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, true, true, UserLevel.SEED, 0);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        OnboardingResultResponse response = onboardingService.getResult("test@test.com");

        assertThat(response.level()).isEqualTo("SEED");
        assertThat(response.totalScore()).isEqualTo(0);
        assertThat(response.levelLabel()).isEqualTo("씨앗");
        assertThat(response.levelEmoji()).isEqualTo("🌱");
        assertThat(response.skipped()).isTrue();
        assertThat(response.onboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("온보딩 미완료 사용자 요청 시 404(ONBOARDING-006) 응답")
    void 온보딩_미완료_사용자_요청_시_404_응답() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, false, false, UserLevel.SEED, 0);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> onboardingService.getResult("test@test.com"))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(be.getCode()).isEqualTo("ONBOARDING-006");
            });
    }

    @Test
    @DisplayName("HIGH_SCHOOL + SEED 추천 도메인 3개 정확성 검증")
    void HIGH_SCHOOL_SEED_추천_도메인_검증() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, true, false, UserLevel.SEED, 0);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        OnboardingResultResponse response = onboardingService.getResult("test@test.com");

        assertThat(response.recommendedDomains()).hasSize(3);
        assertThat(response.recommendedDomains().get(0).id()).isEqualTo("scholarship");
        assertThat(response.recommendedDomains().get(1).id()).isEqualTo("finance");
        assertThat(response.recommendedDomains().get(2).id()).isEqualTo("realestate");
    }

    @Test
    @DisplayName("HIGH_SCHOOL + SPROUT 추천 도메인 3개 정확성 검증")
    void HIGH_SCHOOL_SPROUT_추천_도메인_검증() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, true, false, UserLevel.SPROUT, 5);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        OnboardingResultResponse response = onboardingService.getResult("test@test.com");

        assertThat(response.recommendedDomains()).hasSize(3);
        assertThat(response.recommendedDomains().get(0).id()).isEqualTo("realestate");
        assertThat(response.recommendedDomains().get(1).id()).isEqualTo("finance");
        assertThat(response.recommendedDomains().get(2).id()).isEqualTo("tax");
    }

    @Test
    @DisplayName("HIGH_SCHOOL + TREE 추천 도메인 3개 정확성 검증")
    void HIGH_SCHOOL_TREE_추천_도메인_검증() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, true, false, UserLevel.TREE, 9);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        OnboardingResultResponse response = onboardingService.getResult("test@test.com");

        assertThat(response.recommendedDomains()).hasSize(3);
        assertThat(response.recommendedDomains().get(0).id()).isEqualTo("tax");
        assertThat(response.recommendedDomains().get(1).id()).isEqualTo("finance");
        assertThat(response.recommendedDomains().get(2).id()).isEqualTo("realestate");
    }

    @Test
    @DisplayName("EARLY_CAREER + SEED 추천 도메인 3개 정확성 검증")
    void EARLY_CAREER_SEED_추천_도메인_검증() {
        User user = buildUser("test@test.com", UserType.EARLY_CAREER, true, false, UserLevel.SEED, 0);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        OnboardingResultResponse response = onboardingService.getResult("test@test.com");

        assertThat(response.recommendedDomains()).hasSize(3);
        assertThat(response.recommendedDomains().get(0).id()).isEqualTo("tax");
        assertThat(response.recommendedDomains().get(1).id()).isEqualTo("realestate");
        assertThat(response.recommendedDomains().get(2).id()).isEqualTo("finance");
    }

    @Test
    @DisplayName("EARLY_CAREER + SPROUT 추천 도메인 3개 정확성 검증")
    void EARLY_CAREER_SPROUT_추천_도메인_검증() {
        User user = buildUser("test@test.com", UserType.EARLY_CAREER, true, false, UserLevel.SPROUT, 5);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        OnboardingResultResponse response = onboardingService.getResult("test@test.com");

        assertThat(response.recommendedDomains()).hasSize(3);
        assertThat(response.recommendedDomains().get(0).id()).isEqualTo("finance");
        assertThat(response.recommendedDomains().get(1).id()).isEqualTo("tax");
        assertThat(response.recommendedDomains().get(2).id()).isEqualTo("realestate");
    }

    @Test
    @DisplayName("EARLY_CAREER + TREE 추천 도메인 3개 정확성 검증")
    void EARLY_CAREER_TREE_추천_도메인_검증() {
        User user = buildUser("test@test.com", UserType.EARLY_CAREER, true, false, UserLevel.TREE, 9);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        OnboardingResultResponse response = onboardingService.getResult("test@test.com");

        assertThat(response.recommendedDomains()).hasSize(3);
        assertThat(response.recommendedDomains().get(0).id()).isEqualTo("finance");
        assertThat(response.recommendedDomains().get(1).id()).isEqualTo("tax");
        assertThat(response.recommendedDomains().get(2).id()).isEqualTo("realestate");
    }

    // ── saveInterests ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("관심 도메인 1개 저장 성공")
    void 관심_도메인_1개_저장_성공() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, true, false, UserLevel.SPROUT, 5);
        given(user.getId()).willReturn(1L);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        onboardingService.saveInterests("test@test.com",
            new OnboardingInterestsRequest(List.of("realestate")));

        verify(userInterestRepository).deleteByUserId(1L);
        ArgumentCaptor<List<UserInterest>> captor = ArgumentCaptor.forClass(List.class);
        verify(userInterestRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("관심 도메인 3개 저장 성공")
    void 관심_도메인_3개_저장_성공() {
        User user = buildUser("test@test.com", UserType.HIGH_SCHOOL, true, false, UserLevel.SPROUT, 5);
        given(user.getId()).willReturn(1L);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        onboardingService.saveInterests("test@test.com",
            new OnboardingInterestsRequest(List.of("realestate", "tax", "finance")));

        ArgumentCaptor<List<UserInterest>> captor = ArgumentCaptor.forClass(List.class);
        verify(userInterestRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    @DisplayName("관심 도메인 0개 요청 시 400(ONBOARDING-007) 응답")
    void 관심_도메인_0개_요청_시_400_응답() {
        // 크기 검증이 사용자 조회 전에 수행되므로 userRepository stub 불필요
        assertThatThrownBy(() -> onboardingService.saveInterests("test@test.com",
            new OnboardingInterestsRequest(List.of())))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(be.getCode()).isEqualTo("ONBOARDING-007");
            });
    }

    @Test
    @DisplayName("관심 도메인 4개 이상 요청 시 400(ONBOARDING-007) 응답")
    void 관심_도메인_4개_이상_요청_시_400_응답() {
        assertThatThrownBy(() -> onboardingService.saveInterests("test@test.com",
            new OnboardingInterestsRequest(List.of("realestate", "tax", "finance", "scholarship"))))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(be.getCode()).isEqualTo("ONBOARDING-007");
            });
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private User buildUser(String email, UserType userType, boolean onboardingCompleted,
                           boolean onboardingSkipped, UserLevel level, int score) {
        User user = mock(User.class);
        lenient().when(user.getEmail()).thenReturn(email);
        lenient().when(user.getUserType()).thenReturn(userType);
        lenient().when(user.isOnboardingCompleted()).thenReturn(onboardingCompleted);
        lenient().when(user.isOnboardingSkipped()).thenReturn(onboardingSkipped);
        lenient().when(user.getLevel()).thenReturn(level);
        lenient().when(user.getOnboardingScore()).thenReturn(score);
        return user;
    }

    private List<OnboardingQuestion> buildQuestions(UserType userType, int count) {
        return java.util.stream.LongStream.rangeClosed(1, count)
            .mapToObj(i -> {
                OnboardingQuestion q = mock(OnboardingQuestion.class);
                lenient().when(q.getId()).thenReturn(i);
                lenient().when(q.getUserType()).thenReturn(userType);
                lenient().when(q.getQuestionOrder()).thenReturn((int) i);
                lenient().when(q.getContent()).thenReturn("문제 " + i);
                return q;
            })
            .toList();
    }

    private OnboardingSubmitRequest buildSubmitRequest(List<String> options) {
        List<OnboardingSubmitRequest.Answer> answers =
            java.util.stream.IntStream.range(0, options.size())
                .mapToObj(i -> new OnboardingSubmitRequest.Answer((long) (i + 1), options.get(i)))
                .toList();
        return new OnboardingSubmitRequest(answers);
    }
}

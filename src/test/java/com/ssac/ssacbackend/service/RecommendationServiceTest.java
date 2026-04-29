package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.response.RecommendationResponse;
import com.ssac.ssacbackend.dto.response.RecommendationResponse.RecommendationType;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.QuizRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private QuizRepository quizRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private static final String EMAIL = "user@test.com";

    // ── 신규 사용자 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("응시 기록이 없는 신규 사용자에게는 최신 퀴즈 기본 추천을 반환한다")
    void newUserReturnsDefaultRecommendation() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(mock(User.class)));
        given(quizAttemptRepository.existsByUserEmail(EMAIL)).willReturn(false);

        Quiz q1 = buildQuiz(1L, "퀴즈A");
        Quiz q2 = buildQuiz(2L, "퀴즈B");
        given(quizRepository.findByOrderByCreatedAtDesc(any(Pageable.class)))
            .willReturn(List.of(q1, q2));

        RecommendationResponse result = recommendationService.getRecommendations(EMAIL);

        assertThat(result.personalized()).isFalse();
        assertThat(result.recommendations()).hasSize(2);
        assertThat(result.recommendations())
            .allMatch(r -> r.type() == RecommendationType.DEFAULT);
        assertThat(result.recommendations())
            .allMatch(r -> r.lastAccuracyRate() == null);
    }

    @Test
    @DisplayName("신규 사용자 추천 목록이 비어있으면 빈 추천을 반환한다")
    void newUserNoQuizzesReturnsEmptyList() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(mock(User.class)));
        given(quizAttemptRepository.existsByUserEmail(EMAIL)).willReturn(false);
        given(quizRepository.findByOrderByCreatedAtDesc(any(Pageable.class)))
            .willReturn(List.of());

        RecommendationResponse result = recommendationService.getRecommendations(EMAIL);

        assertThat(result.personalized()).isFalse();
        assertThat(result.recommendations()).isEmpty();
    }

    // ── 기존 사용자 개인화 추천 ───────────────────────────────────────────────

    @Test
    @DisplayName("정답률이 70% 미만인 퀴즈는 RETRY로 추천된다")
    void existingUserLowAccuracyAttemptReturnsRetryRecommendation() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(mock(User.class)));
        given(quizAttemptRepository.existsByUserEmail(EMAIL)).willReturn(true);

        // 5문항 중 2문항 정답 → 정답률 40%
        Quiz quiz = buildQuiz(1L, "어려운 퀴즈");
        QuizAttempt attempt = buildAttempt(quiz, 2, 5);
        given(quizAttemptRepository.findByUserEmailSinceWithQuiz(eq(EMAIL), any(LocalDateTime.class)))
            .willReturn(List.of(attempt));
        given(quizRepository.findUntriedByUserEmail(eq(EMAIL), any(Pageable.class)))
            .willReturn(List.of());

        RecommendationResponse result = recommendationService.getRecommendations(EMAIL);

        assertThat(result.personalized()).isTrue();
        assertThat(result.recommendations()).hasSize(1);
        RecommendationResponse.RecommendedQuizResponse item = result.recommendations().get(0);
        assertThat(item.type()).isEqualTo(RecommendationType.RETRY);
        assertThat(item.lastAccuracyRate()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("정답률이 70% 이상인 퀴즈는 RETRY로 추천되지 않는다")
    void existingUserHighAccuracyAttemptNotRecommendedAsRetry() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(mock(User.class)));
        given(quizAttemptRepository.existsByUserEmail(EMAIL)).willReturn(true);

        // 10문항 중 8문항 정답 → 정답률 80%
        Quiz quiz = buildQuiz(1L, "잘 푼 퀴즈");
        QuizAttempt attempt = buildAttempt(quiz, 8, 10);
        given(quizAttemptRepository.findByUserEmailSinceWithQuiz(eq(EMAIL), any(LocalDateTime.class)))
            .willReturn(List.of(attempt));
        given(quizRepository.findUntriedByUserEmail(eq(EMAIL), any(Pageable.class)))
            .willReturn(List.of());

        RecommendationResponse result = recommendationService.getRecommendations(EMAIL);

        assertThat(result.personalized()).isTrue();
        assertThat(result.recommendations())
            .noneMatch(r -> r.type() == RecommendationType.RETRY);
    }

    @Test
    @DisplayName("미시도 퀴즈는 UNTRIED로 추천된다")
    void existingUserUntriedQuizReturnsUntriedRecommendation() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(mock(User.class)));
        given(quizAttemptRepository.existsByUserEmail(EMAIL)).willReturn(true);
        given(quizAttemptRepository.findByUserEmailSinceWithQuiz(eq(EMAIL), any(LocalDateTime.class)))
            .willReturn(List.of());

        Quiz untried = buildQuiz(10L, "새로운 퀴즈");
        given(quizRepository.findUntriedByUserEmail(eq(EMAIL), any(Pageable.class)))
            .willReturn(List.of(untried));

        RecommendationResponse result = recommendationService.getRecommendations(EMAIL);

        assertThat(result.personalized()).isTrue();
        assertThat(result.recommendations()).hasSize(1);
        assertThat(result.recommendations().get(0).type()).isEqualTo(RecommendationType.UNTRIED);
        assertThat(result.recommendations().get(0).lastAccuracyRate()).isNull();
    }

    @Test
    @DisplayName("RETRY와 UNTRIED가 동시에 존재하면 함께 반환된다")
    void existingUserMixedRecommendations() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(mock(User.class)));
        given(quizAttemptRepository.existsByUserEmail(EMAIL)).willReturn(true);

        Quiz weakQuiz = buildQuiz(1L, "약점 퀴즈");
        QuizAttempt weakAttempt = buildAttempt(weakQuiz, 1, 5); // 20%
        given(quizAttemptRepository.findByUserEmailSinceWithQuiz(eq(EMAIL), any(LocalDateTime.class)))
            .willReturn(List.of(weakAttempt));

        Quiz newQuiz = buildQuiz(2L, "새 퀴즈");
        given(quizRepository.findUntriedByUserEmail(eq(EMAIL), any(Pageable.class)))
            .willReturn(List.of(newQuiz));

        RecommendationResponse result = recommendationService.getRecommendations(EMAIL);

        assertThat(result.personalized()).isTrue();
        assertThat(result.recommendations()).hasSize(2);
        assertThat(result.recommendations())
            .anyMatch(r -> r.type() == RecommendationType.RETRY);
        assertThat(result.recommendations())
            .anyMatch(r -> r.type() == RecommendationType.UNTRIED);
    }

    @Test
    @DisplayName("동일 퀴즈가 RETRY와 UNTRIED 양쪽에 해당해도 중복 포함되지 않는다")
    void existingUserSameQuizNotDuplicated() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(mock(User.class)));
        given(quizAttemptRepository.existsByUserEmail(EMAIL)).willReturn(true);

        Quiz quiz = buildQuiz(1L, "겹치는 퀴즈");
        QuizAttempt weakAttempt = buildAttempt(quiz, 1, 5); // 20% → RETRY
        given(quizAttemptRepository.findByUserEmailSinceWithQuiz(eq(EMAIL), any(LocalDateTime.class)))
            .willReturn(List.of(weakAttempt));
        // 동일한 quiz ID가 findUntriedByUserEmail 에도 반환된다고 가정 (경계 케이스)
        given(quizRepository.findUntriedByUserEmail(eq(EMAIL), any(Pageable.class)))
            .willReturn(List.of(quiz));

        RecommendationResponse result = recommendationService.getRecommendations(EMAIL);

        long count = result.recommendations().stream()
            .filter(r -> r.quizId().equals(1L))
            .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 이메일로 조회하면 404 예외가 발생한다")
    void unknownEmailThrowsNotFoundException() {
        given(userRepository.findByEmail("none@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getRecommendations("none@test.com"))
            .isInstanceOf(BusinessException.class);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Quiz buildQuiz(Long id, String title) {
        Quiz quiz = mock(Quiz.class);
        Mockito.lenient().when(quiz.getId()).thenReturn(id);
        Mockito.lenient().when(quiz.getTitle()).thenReturn(title);
        Mockito.lenient().when(quiz.getDescription()).thenReturn("설명");
        Mockito.lenient().when(quiz.getMaxScore()).thenReturn(100);
        Mockito.lenient().when(quiz.getTotalQuestions()).thenReturn(10);
        return quiz;
    }

    private QuizAttempt buildAttempt(Quiz quiz, int correctCount, int totalQuestions) {
        QuizAttempt attempt = mock(QuizAttempt.class);
        Mockito.lenient().when(attempt.getQuiz()).thenReturn(quiz);
        Mockito.lenient().when(attempt.getCorrectCount()).thenReturn(correctCount);
        Mockito.lenient().when(quiz.getTotalQuestions()).thenReturn(totalQuestions);
        return attempt;
    }
}

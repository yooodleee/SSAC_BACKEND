package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.content.ContentDifficulty;
import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.domain.user.LevelHistory;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.response.LevelUpResult;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.LevelHistoryRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LevelUpServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentProgressRepository contentProgressRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private LevelHistoryRepository levelHistoryRepository;

    @InjectMocks
    private LevelUpService levelUpService;

    private User buildUser(UserLevel level) {
        User user = User.builder()
            .email("test@example.com")
            .nickname("tester")
            .role(UserRole.USER)
            .build();
        user.completeOnboarding(level, 8);
        return user;
    }

    private QuizAttempt buildAttempt(int totalQuestions, int correctCount) {
        Quiz quiz = mock(Quiz.class);
        given(quiz.getTotalQuestions()).willReturn(totalQuestions);

        QuizAttempt attempt = mock(QuizAttempt.class);
        given(attempt.getQuiz()).willReturn(quiz);
        given(attempt.getCorrectCount()).willReturn(correctCount);
        return attempt;
    }

    @Test
    @DisplayName("SEED → SPROUT 레벨업 조건 충족 시 자동 레벨업")
    void checkAndApplyLevelUp_seedToSprout() {
        User user = buildUser(UserLevel.SEED);
        QuizAttempt attempt = buildAttempt(10, 8);
        // contentRate 80% (8/10), quizRate 80% (8/10) >= SEED threshold 70%
        given(contentRepository.countByDifficulty(ContentDifficulty.SEED)).willReturn(10L);
        given(contentProgressRepository.countCompletedByUserEmailAndDifficulty(
            "test@example.com", ContentDifficulty.SEED)).willReturn(8L);
        given(quizAttemptRepository.findRecentByUserEmail(
            any(String.class), any(Pageable.class)))
            .willReturn(List.of(attempt));
        given(levelHistoryRepository.save(any(LevelHistory.class))).willAnswer(inv -> inv.getArgument(0));

        LevelUpResult result = levelUpService.checkAndApplyLevelUp(user, "test@example.com");

        assertThat(result.leveledUp()).isTrue();
        assertThat(result.previousLevel()).isEqualTo(UserLevel.SEED);
        assertThat(result.newLevel()).isEqualTo(UserLevel.SPROUT);
        assertThat(user.getLevel()).isEqualTo(UserLevel.SPROUT);
    }

    @Test
    @DisplayName("SPROUT → TREE 레벨업 조건 충족 시 자동 레벨업")
    void checkAndApplyLevelUp_sproutToTree() {
        User user = buildUser(UserLevel.SPROUT);
        QuizAttempt attempt = buildAttempt(10, 9);
        // contentRate 90% (9/10), quizRate 90% (9/10) >= SPROUT threshold 80%
        given(contentRepository.countByDifficulty(ContentDifficulty.SPROUT)).willReturn(10L);
        given(contentProgressRepository.countCompletedByUserEmailAndDifficulty(
            "test@example.com", ContentDifficulty.SPROUT)).willReturn(9L);
        given(quizAttemptRepository.findRecentByUserEmail(
            any(String.class), any(Pageable.class)))
            .willReturn(List.of(attempt));
        given(levelHistoryRepository.save(any(LevelHistory.class))).willAnswer(inv -> inv.getArgument(0));

        LevelUpResult result = levelUpService.checkAndApplyLevelUp(user, "test@example.com");

        assertThat(result.leveledUp()).isTrue();
        assertThat(result.previousLevel()).isEqualTo(UserLevel.SPROUT);
        assertThat(result.newLevel()).isEqualTo(UserLevel.TREE);
        assertThat(user.getLevel()).isEqualTo(UserLevel.TREE);
    }

    @Test
    @DisplayName("레벨업 조건 미충족 시 isLevelUp false 반환")
    void checkAndApplyLevelUp_conditionsNotMet_noLevelUp() {
        User user = buildUser(UserLevel.SEED);
        QuizAttempt attempt = buildAttempt(10, 6);
        // contentRate 80%, quizRate 60% < SEED threshold 70% → no levelup
        given(contentRepository.countByDifficulty(ContentDifficulty.SEED)).willReturn(10L);
        given(contentProgressRepository.countCompletedByUserEmailAndDifficulty(
            "test@example.com", ContentDifficulty.SEED)).willReturn(8L);
        given(quizAttemptRepository.findRecentByUserEmail(
            any(String.class), any(Pageable.class)))
            .willReturn(List.of(attempt));

        LevelUpResult result = levelUpService.checkAndApplyLevelUp(user, "test@example.com");

        assertThat(result.leveledUp()).isFalse();
        assertThat(user.getLevel()).isEqualTo(UserLevel.SEED);
        verify(levelHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("TREE 레벨 사용자는 레벨업 판정 없이 false 반환")
    void checkAndApplyLevelUp_treeLevel_skipsCheck() {
        User user = buildUser(UserLevel.TREE);

        LevelUpResult result = levelUpService.checkAndApplyLevelUp(user, "test@example.com");

        assertThat(result.leveledUp()).isFalse();
        verify(contentRepository, never()).countByDifficulty(any());
        verify(levelHistoryRepository, never()).save(any());
    }
}

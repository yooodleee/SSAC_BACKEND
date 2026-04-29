package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.repository.MigrationFailureRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestMigrationServiceTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private MigrationFailureRepository migrationFailureRepository;

    @InjectMocks
    private GuestMigrationService guestMigrationService;

    @Test
    @DisplayName("중복 없는 경우 모든 Guest 기록을 User로 이전하고 true를 반환한다")
    void migrateGuestDataNoDuplicatesTransfersAllToUser() {
        String guestId = "test-guest-uuid";
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);

        Quiz quiz1 = mock(Quiz.class);
        given(quiz1.getId()).willReturn(1L);
        Quiz quiz2 = mock(Quiz.class);
        given(quiz2.getId()).willReturn(2L);

        QuizAttempt attempt1 = mock(QuizAttempt.class);
        given(attempt1.getQuiz()).willReturn(quiz1);
        QuizAttempt attempt2 = mock(QuizAttempt.class);
        given(attempt2.getQuiz()).willReturn(quiz2);

        given(quizAttemptRepository.findByGuestIdWithQuiz(guestId))
            .willReturn(List.of(attempt1, attempt2));
        given(quizAttemptRepository.findByUserAndQuizIds(user, List.of(1L, 2L)))
            .willReturn(List.of());

        boolean result = guestMigrationService.migrateGuestData(guestId, user);

        assertThat(result).isTrue();
        verify(attempt1).transferToUser(user);
        verify(attempt2).transferToUser(user);
    }

    @Test
    @DisplayName("Guest 기록이 없으면 아무 작업도 하지 않고 true를 반환한다")
    void migrateGuestDataNoRecordsReturnsTrueWithoutAction() {
        String guestId = "test-guest-uuid";
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(quizAttemptRepository.findByGuestIdWithQuiz(guestId)).willReturn(List.of());

        boolean result = guestMigrationService.migrateGuestData(guestId, user);

        assertThat(result).isTrue();
        verify(quizAttemptRepository, never()).deleteAll(anyList());
    }

    @Test
    @DisplayName("Guest 기록이 더 최신이면 Guest 기록을 이전하고 기존 User 기록을 삭제한다")
    void migrateGuestDataGuestIsNewerTransfersGuestAndDeletesUser() {
        String guestId = "test-guest-uuid";
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);

        Quiz quiz = mock(Quiz.class);
        given(quiz.getId()).willReturn(1L);

        LocalDateTime older = LocalDateTime.now().minusDays(1);
        LocalDateTime newer = LocalDateTime.now();

        QuizAttempt guestAttempt = mock(QuizAttempt.class);
        given(guestAttempt.getQuiz()).willReturn(quiz);
        given(guestAttempt.getAttemptedAt()).willReturn(newer);

        QuizAttempt userAttempt = mock(QuizAttempt.class);
        given(userAttempt.getQuiz()).willReturn(quiz);
        given(userAttempt.getAttemptedAt()).willReturn(older);

        given(quizAttemptRepository.findByGuestIdWithQuiz(guestId)).willReturn(List.of(guestAttempt));
        given(quizAttemptRepository.findByUserAndQuizIds(user, List.of(1L))).willReturn(List.of(userAttempt));

        boolean result = guestMigrationService.migrateGuestData(guestId, user);

        assertThat(result).isTrue();
        verify(guestAttempt).transferToUser(user);
        verify(quizAttemptRepository).deleteAll(List.of(userAttempt));
    }

    @Test
    @DisplayName("User 기록이 더 최신이면 Guest 기록을 삭제하고 User 기록을 유지한다")
    void migrateGuestDataUserIsNewerDeletesGuestRecord() {
        String guestId = "test-guest-uuid";
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);

        Quiz quiz = mock(Quiz.class);
        given(quiz.getId()).willReturn(1L);

        LocalDateTime older = LocalDateTime.now().minusDays(1);
        LocalDateTime newer = LocalDateTime.now();

        QuizAttempt guestAttempt = mock(QuizAttempt.class);
        given(guestAttempt.getQuiz()).willReturn(quiz);
        given(guestAttempt.getAttemptedAt()).willReturn(older);

        QuizAttempt userAttempt = mock(QuizAttempt.class);
        given(userAttempt.getQuiz()).willReturn(quiz);
        given(userAttempt.getAttemptedAt()).willReturn(newer);

        given(quizAttemptRepository.findByGuestIdWithQuiz(guestId)).willReturn(List.of(guestAttempt));
        given(quizAttemptRepository.findByUserAndQuizIds(user, List.of(1L))).willReturn(List.of(userAttempt));

        boolean result = guestMigrationService.migrateGuestData(guestId, user);

        assertThat(result).isTrue();
        verify(guestAttempt, never()).transferToUser(user);
        verify(quizAttemptRepository).deleteAll(List.of(guestAttempt));
    }

    @Test
    @DisplayName("마이그레이션 중 예외 발생 시 실패 정보를 기록하고 false를 반환한다")
    void migrateGuestDataOnExceptionRecordsFailureAndReturnsFalse() {
        String guestId = "test-guest-uuid";
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);

        // findByGuestIdWithQuiz에서 예외가 발생하면 catch 블록으로 이동해야 함
        given(quizAttemptRepository.findByGuestIdWithQuiz(guestId))
            .willAnswer(invocation -> {
                throw new RuntimeException("DB Error");
            });

        boolean result = guestMigrationService.migrateGuestData(guestId, user);

        assertThat(result).isFalse();
        verify(migrationFailureRepository).save(any());
    }
}

package com.ssac.ssacbackend.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
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

    @InjectMocks
    private GuestMigrationService guestMigrationService;

    @Test
    @DisplayName("guestId로 저장된 응시 기록이 있으면 각 기록을 User로 이전한다")
    void migrateGuestDataWithRecordsTransfersAllToUser() {
        String guestId = "test-guest-uuid";
        User user = mock(User.class);
        QuizAttempt attempt1 = mock(QuizAttempt.class);
        QuizAttempt attempt2 = mock(QuizAttempt.class);
        given(quizAttemptRepository.findByGuestId(guestId)).willReturn(List.of(attempt1, attempt2));

        guestMigrationService.migrateGuestData(guestId, user);

        verify(attempt1).transferToUser(user);
        verify(attempt2).transferToUser(user);
    }

    @Test
    @DisplayName("guestId로 저장된 응시 기록이 없으면 transferToUser를 호출하지 않는다")
    void migrateGuestDataNoRecordsDoesNotCallTransfer() {
        String guestId = "test-guest-uuid";
        User user = mock(User.class);
        given(quizAttemptRepository.findByGuestId(guestId)).willReturn(List.of());

        guestMigrationService.migrateGuestData(guestId, user);

        verify(user, never()).updateNickname(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("마이그레이션 대상 guestId로만 조회하고 다른 guestId의 기록은 건드리지 않는다")
    void migrateGuestDataQueriesOnlyTargetGuestId() {
        String guestId = "target-guest";
        User user = mock(User.class);
        given(quizAttemptRepository.findByGuestId(guestId)).willReturn(List.of());

        guestMigrationService.migrateGuestData(guestId, user);

        verify(quizAttemptRepository).findByGuestId(guestId);
        verify(quizAttemptRepository, never()).findByGuestId("other-guest");
    }
}

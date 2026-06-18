package com.ssac.ssacbackend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GuestDataCleanupService 단위 테스트.
 *
 * <p>30일 초과 비회원 데이터 삭제 호출 및 예외 처리를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class GuestDataCleanupServiceTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @InjectMocks
    private GuestDataCleanupService service;

    @Test
    @DisplayName("30일 초과 비회원 데이터를 삭제한다")
    void cleanupExpiredGuestData_삭제_호출() {
        willDoNothing().given(quizAttemptRepository)
            .deleteByGuestIdIsNotNullAndAttemptedAtBefore(any(LocalDateTime.class));

        service.cleanupExpiredGuestData();

        verify(quizAttemptRepository)
            .deleteByGuestIdIsNotNullAndAttemptedAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Repository 예외 발생 시 예외가 외부로 전파되지 않는다")
    void cleanupExpiredGuestData_예외_발생_시_전파_안됨() {
        willThrow(new RuntimeException("DB 오류"))
            .given(quizAttemptRepository)
            .deleteByGuestIdIsNotNullAndAttemptedAtBefore(any(LocalDateTime.class));

        // 예외가 전파되지 않으면 테스트 통과
        service.cleanupExpiredGuestData();
    }
}

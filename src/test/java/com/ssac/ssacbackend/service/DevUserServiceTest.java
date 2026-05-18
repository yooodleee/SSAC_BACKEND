package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.MigrationFailureRepository;
import com.ssac.ssacbackend.repository.NotificationRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.RefreshTokenRepository;
import com.ssac.ssacbackend.repository.SocialAccountRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DevUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock SocialAccountRepository socialAccountRepository;
    @Mock QuizAttemptRepository quizAttemptRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock ContentProgressRepository contentProgressRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock MigrationFailureRepository migrationFailureRepository;
    @Mock UserInterestRepository userInterestRepository;

    @InjectMocks
    DevUserService devUserService;

    @Test
    @DisplayName("정상 삭제: user_interests 포함 모든 연관 데이터 삭제 후 User 삭제")
    void deleteByEmail_삭제_성공() {
        // given
        String email = "test@example.com";
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        devUserService.deleteByEmail(email);

        // then
        then(socialAccountRepository).should().deleteByUser(user);
        then(quizAttemptRepository).should().deleteByUser(user);
        then(notificationRepository).should().deleteByUserEmail(email);
        then(contentProgressRepository).should().deleteByUserEmail(email);
        then(refreshTokenRepository).should().deleteByUserId(1L);
        then(migrationFailureRepository).should().deleteByUserId(1L);
        then(userInterestRepository).should().deleteByUserId(1L);
        then(userRepository).should().delete(user);
    }

    @Test
    @DisplayName("user_interests 삭제가 userRepository.delete() 보다 먼저 호출된다 (FK 제약 준수)")
    void deleteByEmail_userInterest_삭제_순서_검증() {
        // given
        String email = "test@example.com";
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getId()).willReturn(2L);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
            userInterestRepository, userRepository);

        // when
        devUserService.deleteByEmail(email);

        // then
        inOrder.verify(userInterestRepository).deleteByUserId(2L);
        inOrder.verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 NotFoundException 발생, 삭제 없음")
    void deleteByEmail_사용자_없음_예외() {
        // given
        String email = "nonexistent@example.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> devUserService.deleteByEmail(email))
            .isInstanceOf(NotFoundException.class);

        then(userRepository).should(never()).delete(org.mockito.ArgumentMatchers.any());
        then(userInterestRepository).should(never()).deleteByUserId(org.mockito.ArgumentMatchers.any());
    }
}

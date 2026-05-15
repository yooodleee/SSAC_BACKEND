package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.response.MyPageResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private ContentProgressRepository contentProgressRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @InjectMocks
    private UserService userService;

    private User buildUser(UserLevel level, boolean onboardingCompleted) {
        User user = User.builder()
            .email("test@example.com")
            .nickname("tester")
            .role(UserRole.USER)
            .build();
        if (level != null) {
            user.completeOnboarding(level, 8);
        }
        if (!onboardingCompleted) {
            user.resetOnboarding();
        }
        return user;
    }

    @Test
    @DisplayName("마이페이지 프로필 조회 성공")
    void getMyPage_success() {
        User user = buildUser(UserLevel.SEED, true);
        user.setUserType(UserType.HIGH_SCHOOL);

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(userInterestRepository.findDomainIdsByUserId(any())).willReturn(List.of("finance", "tax"));
        given(contentProgressRepository.countByUserEmailAndProgressRateGreaterThanEqual(
            "test@example.com", 100)).willReturn(3L);
        given(quizAttemptRepository.aggregateOverallStats("test@example.com"))
            .willReturn(Collections.singletonList(new Object[]{100L, 5L, 20L, 25L}));
        given(contentProgressRepository.findActivityTimestampsByUserEmail("test@example.com"))
            .willReturn(List.of(LocalDateTime.now()));
        given(quizAttemptRepository.findActivityTimestampsByUserEmail("test@example.com"))
            .willReturn(List.of());

        MyPageResponse response = userService.getMyPage("test@example.com");

        assertThat(response).isNotNull();
        assertThat(response.nickname()).isEqualTo("tester");
        assertThat(response.level()).isEqualTo("SEED");
        assertThat(response.interests()).containsExactly("finance", "tax");
        assertThat(response.stats().totalContentsCompleted()).isEqualTo(3L);
        assertThat(response.stats().totalQuizCompleted()).isEqualTo(5L);
    }

    @Test
    @DisplayName("관심 도메인 수정 성공")
    void updateInterests_success() {
        User user = buildUser(UserLevel.SEED, true);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        userService.updateInterests("test@example.com", List.of("finance", "tax"));

        verify(userInterestRepository).deleteByUserId(any());
        verify(userInterestRepository).saveAll(any());
    }

    @Test
    @DisplayName("관심 도메인 개수 초과 시 BadRequestException")
    void updateInterests_tooMany_throwsBadRequest() {
        assertThatThrownBy(() ->
            userService.updateInterests("test@example.com", List.of("a", "b", "c", "d")))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("사용자 유형 변경 성공 - 온보딩 미완료 상태")
    void updateUserType_notOnboarded_success() {
        User user = buildUser(null, false);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        userService.updateUserType("test@example.com", UserType.EARLY_CAREER);

        assertThat(user.getUserType()).isEqualTo(UserType.EARLY_CAREER);
        assertThat(user.isOnboardingCompleted()).isFalse();
    }

    @Test
    @DisplayName("사용자 유형 변경 시 온보딩 완료 상태면 초기화")
    void updateUserType_onboardingCompleted_resetsOnboarding() {
        User user = buildUser(UserLevel.SPROUT, true);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        userService.updateUserType("test@example.com", UserType.EARLY_CAREER);

        assertThat(user.getUserType()).isEqualTo(UserType.EARLY_CAREER);
        assertThat(user.isOnboardingCompleted()).isFalse();
        assertThat(user.getLevel()).isNull();
        verify(userInterestRepository).deleteByUserId(any());
    }
}

package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.common.exception.ConflictException;
import com.ssac.ssacbackend.domain.user.Gender;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.request.UpdateProfileRequest;
import com.ssac.ssacbackend.dto.response.MyPageResponse;
import com.ssac.ssacbackend.dto.response.UpdateProfileResponse;
import com.ssac.ssacbackend.dto.response.ViewedContentsResponse;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.QuizAttemptService.MyPageQuizStats;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * UserService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserInterestRepository userInterestRepository;
    @Mock
    private HomeCacheEvictService homeCacheEvictService;
    @Mock
    private TokenService tokenService;
    @Mock
    private ContentService contentService;
    @Mock
    private QuizAttemptService quizAttemptService;

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

    private User buildUserWithProfile() {
        User user = User.builder()
            .email("test@example.com")
            .nickname("testnick")
            .password("pw")
            .build();
        user.completeSignup("홍길동", LocalDate.of(2000, 1, 1), "01012345678", Gender.MALE);
        return user;
    }

    @Test
    @DisplayName("마이페이지 프로필 조회 성공")
    void getMyPage_success() {
        User user = buildUser(UserLevel.SEED, true);
        user.setUserType(UserType.HIGH_SCHOOL);

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(userInterestRepository.findDomainIdsByUserId(any())).willReturn(List.of("finance", "tax"));
        given(contentService.countCompletedContents("test@example.com")).willReturn(3L);
        given(quizAttemptService.getMyPageQuizStats("test@example.com"))
            .willReturn(new MyPageQuizStats(5L, 80));
        given(contentService.findActivityTimestamps("test@example.com"))
            .willReturn(List.of(LocalDateTime.now()));
        given(quizAttemptService.findActivityTimestamps("test@example.com"))
            .willReturn(List.of());

        MyPageResponse response = userService.getMyPage("test@example.com");

        assertThat(response).isNotNull();
        assertThat(response.nickname()).isEqualTo("tester");
        assertThat(response.level()).isEqualTo("SEED");
        assertThat(response.interests()).containsExactly("finance", "tax");
        assertThat(response.stats().totalContentsCompleted()).isEqualTo(3L);
        assertThat(response.stats().totalQuizCompleted()).isEqualTo(5L);
        assertThat(response.stats().correctRate()).isEqualTo(80);
    }

    @Test
    @DisplayName("휴대폰 번호 하이픈 포함 형식으로 응답한다")
    void getMyPage_phoneWithHyphen() {
        User user = buildUserWithProfile();

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(userInterestRepository.findDomainIdsByUserId(any())).willReturn(List.of());
        given(contentService.countCompletedContents(any())).willReturn(0L);
        given(quizAttemptService.getMyPageQuizStats(any())).willReturn(new MyPageQuizStats(0L, 0));
        given(contentService.findActivityTimestamps(any())).willReturn(List.of());
        given(quizAttemptService.findActivityTimestamps(any())).willReturn(List.of());

        MyPageResponse response = userService.getMyPage("test@example.com");

        assertThat(response.phone()).isEqualTo("010-1234-5678");
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

    @Test
    @DisplayName("개인정보 name만 포함된 요청 시 name만 수정된다")
    void updateProfile_onlyName() {
        User user = buildUserWithProfile();
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        UpdateProfileRequest request = new UpdateProfileRequest("새이름", null, null, null, null);

        UpdateProfileResponse response = userService.updateProfile("test@example.com", request);

        assertThat(response.name()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("이름 20자 초과 시 NAME-002 에러")
    void updateProfile_nameTooLong() {
        User user = buildUserWithProfile();
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        UpdateProfileRequest request = new UpdateProfileRequest("a".repeat(21), null, null, null, null);

        assertThatThrownBy(() -> userService.updateProfile("test@example.com", request))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("잘못된 전화번호 형식 시 PHONE-001 에러")
    void updateProfile_invalidPhone() {
        User user = buildUserWithProfile();
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, "01012345678", null, null);

        assertThatThrownBy(() -> userService.updateProfile("test@example.com", request))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("중복 이메일 변경 시 EMAIL-002 에러")
    void updateProfile_duplicateEmail() {
        User user = buildUserWithProfile();
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("other@example.com")).willReturn(true);
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, null, null, "other@example.com");

        assertThatThrownBy(() -> userService.updateProfile("test@example.com", request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("내가 본 콘텐츠 이력이 없으면 빈 리스트 반환")
    void getViewedContents_emptyList() {
        User user = buildUserWithProfile();
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(contentService.getViewedContentsByUser(any())).willReturn(List.of());

        ViewedContentsResponse response = userService.getViewedContents("test@example.com");

        assertThat(response.contents()).isEmpty();
        assertThat(response.totalCount()).isZero();
    }
}

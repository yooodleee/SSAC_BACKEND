package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.response.ProfileResponse;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    // ── 프로필 조회 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("존재하는 이메일로 프로필 조회 시 ProfileResponse를 반환한다")
    void 존재하는_이메일_프로필_조회_성공() {
        User user = buildUser("user@test.com", "닉네임A");
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));

        ProfileResponse response = profileService.getProfile("user@test.com");

        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.nickname()).isEqualTo("닉네임A");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 프로필 조회 시 BusinessException(NOT_FOUND)이 발생한다")
    void 존재하지_않는_이메일_프로필_조회_시_예외_발생() {
        given(userRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile("ghost@test.com"))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── 닉네임 수정 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("현재 닉네임과 동일한 값으로 수정 시 중복 검사 없이 성공한다")
    void 동일한_닉네임_수정_중복_검사_스킵() {
        User user = buildUser("user@test.com", "같은닉네임");
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));

        profileService.updateNickname("user@test.com", "같은닉네임");

        then(userRepository).should(never()).existsByNickname("같은닉네임");
    }

    @Test
    @DisplayName("다른 사용자가 사용하지 않는 닉네임으로 수정 시 성공한다")
    void 유니크한_새_닉네임으로_수정_성공() {
        User user = buildUser("user@test.com", "기존닉네임");
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("새닉네임")).willReturn(false);

        profileService.updateNickname("user@test.com", "새닉네임");

        then(user).should().updateNickname("새닉네임");
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 수정 시 BusinessException(CONFLICT)이 발생한다")
    void 중복_닉네임_수정_시_예외_발생() {
        User user = buildUser("user@test.com", "기존닉네임");
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);

        assertThatThrownBy(() -> profileService.updateNickname("user@test.com", "중복닉네임"))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 닉네임 수정 시 BusinessException(NOT_FOUND)이 발생한다")
    void 존재하지_않는_사용자_닉네임_수정_시_예외_발생() {
        given(userRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateNickname("ghost@test.com", "닉네임"))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private User buildUser(String email, String nickname) {
        User user = mock(User.class);
        given(user.getEmail()).willReturn(email);
        given(user.getNickname()).willReturn(nickname);
        return user;
    }
}

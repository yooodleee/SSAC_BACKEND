package com.ssac.ssacbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.response.OAuth2UserInfo;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("신규 소셜 사용자가 로그인하면 새로운 유저를 생성하고 저장한다")
    void saveNewUser() {
        // given
        OAuth2UserInfo userInfo = createMockUserInfo("new@kakao.com", "신규유저", "kakao", "12345");
        
        given(userRepository.findByProviderAndProviderId("kakao", "12345"))
            .willReturn(Optional.empty());
        given(userRepository.existsByNickname("신규유저")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        User savedUser = customOAuth2UserService.saveOrUpdate(userInfo);

        // then
        assertThat(savedUser.getEmail()).isEqualTo("new@kakao.com");
        assertThat(savedUser.getNickname()).isEqualTo("신규유저");
        assertThat(savedUser.getProvider()).isEqualTo("kakao");
        assertThat(savedUser.getProviderId()).isEqualTo("12345");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("이미 가입된 소셜 사용자가 로그인하면 닉네임을 업데이트한다")
    void updateExistingUser() {
        // given
        User existingUser = User.builder()
            .email("old@kakao.com")
            .nickname("옛날닉네임")
            .provider("kakao")
            .providerId("12345")
            .build();

        OAuth2UserInfo newUserInfo = createMockUserInfo("old@kakao.com", "새로운닉네임", "kakao", "12345");

        given(userRepository.findByProviderAndProviderId("kakao", "12345"))
            .willReturn(Optional.of(existingUser));

        // when
        User updatedUser = customOAuth2UserService.saveOrUpdate(newUserInfo);

        // then
        assertThat(updatedUser.getNickname()).isEqualTo("새로운닉네임");
        assertThat(updatedUser.getEmail()).isEqualTo("old@kakao.com");
    }

    @Test
    @DisplayName("닉네임이 중복되면 뒤에 랜덤 문자열을 붙여 저장한다")
    void handleNicknameCollision() {
        // given
        OAuth2UserInfo userInfo = createMockUserInfo("collision@kakao.com", "중복닉네임", "kakao", "999");

        given(userRepository.findByProviderAndProviderId("kakao", "999"))
            .willReturn(Optional.empty());
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        User savedUser = customOAuth2UserService.saveOrUpdate(userInfo);

        // then
        assertThat(savedUser.getNickname()).startsWith("중복닉네임_");
        assertThat(savedUser.getNickname().length()).isGreaterThan("중복닉네임_".length());
    }

    @Test
    @DisplayName("이미 다른 방식으로 가입된 이메일로 소셜 로그인을 시도하면 예외가 발생한다")
    void throwExceptionWhenEmailAlreadyExists() {
        // given
        OAuth2UserInfo userInfo = createMockUserInfo("existing@ssac.com", "소셜유저", "kakao", "12345");

        given(userRepository.findByProviderAndProviderId("kakao", "12345"))
            .willReturn(Optional.empty());
        // 동일한 이메일을 가진 기존 사용자가 존재함
        given(userRepository.findByEmail("existing@ssac.com"))
            .willReturn(Optional.of(User.builder().email("existing@ssac.com").nickname("기존유저").build()));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class, () -> {
            customOAuth2UserService.saveOrUpdate(userInfo);
        });
    }

    private OAuth2UserInfo createMockUserInfo(String email, String nickname, String provider, String providerId) {
        return new OAuth2UserInfo() {
            @Override
            public String getProviderId() { return providerId; }
            @Override
            public String getProvider() { return provider; }
            @Override
            public String getEmail() { return email; }
            @Override
            public String getNickname() { return nickname; }
        };
    }
}

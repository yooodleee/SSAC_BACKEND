package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.response.KakaoUserInfo;
import com.ssac.ssacbackend.dto.response.OAuth2UserInfo;
import com.ssac.ssacbackend.repository.UserRepository;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 로그인 성공 시 사용자 정보를 DB에 저장하거나 업데이트한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
       ` OAuth2UserInfo oAuth2UserInfo = null;

        if (registrationId.equals("kakao")) {
            oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        }

        if (oAuth2UserInfo == null) {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        User user = saveOrUpdate(oAuth2UserInfo);

        return new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
            oAuth2User.getAttributes(),
            userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
        );
    }

    User saveOrUpdate(OAuth2UserInfo oAuth2UserInfo) {
        String email = oAuth2UserInfo.getEmail();
        if (email == null) {
            email = oAuth2UserInfo.getProviderId() + "@kakao.com";
        }

        final String finalEmail = email;

        return userRepository.findByProviderAndProviderId(oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId())
            .map(entity -> {
                entity.updateNickname(oAuth2UserInfo.getNickname());
                return entity;
            })
            .orElseGet(() -> {
                // 이메일 중복 체크: 이미 다른 방식으로 가입된 이메일인지 확인
                userRepository.findByEmail(finalEmail).ifPresent(existingUser -> {
                    throw new OAuth2AuthenticationException("이미 가입된 이메일입니다. 기존 계정으로 로그인해주세요.");
                });

                String nickname = oAuth2UserInfo.getNickname();
                if (userRepository.existsByNickname(nickname)) {
                    nickname = nickname + "_" + UUID.randomUUID().toString().substring(0, 5);
                }

                return userRepository.save(User.builder()
                    .email(finalEmail)
                    .nickname(nickname)
                    .provider(oAuth2UserInfo.getProvider())
                    .providerId(oAuth2UserInfo.getProviderId())
                    .build());
            });
    }
}

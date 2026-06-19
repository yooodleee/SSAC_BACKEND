package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.UnauthorizedException;
import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.RegisterV2Result;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.request.EmailLoginRequest;
import com.ssac.ssacbackend.dto.response.RegisterV2Response;
import com.ssac.ssacbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 이메일+비밀번호 로그인 서비스.
 *
 * <p>소셜 로그인이 아닌 이메일 직접 인증 경로를 담당한다.
 * 이메일 미존재와 비밀번호 불일치를 동일한 에러(AUTH-011)로 응답하여 계정 열거 공격을 방지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    /**
     * 이메일+비밀번호로 로그인한다.
     *
     * @throws UnauthorizedException 이메일 미존재 또는 비밀번호 불일치 (AUTH-011)
     */
    public RegisterV2Result loginWithEmail(EmailLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.EMAIL_OR_PASSWORD_INVALID));

        if (user.getPassword() == null
            || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException(ErrorCode.EMAIL_OR_PASSWORD_INVALID);
        }

        TokenPair tokenPair = tokenService.issueTokens(user);
        log.info("이메일 로그인 완료: userId={}", user.getId());

        RegisterV2Response response = new RegisterV2Response(
            tokenPair.accessToken(),
            "Bearer",
            jwtProperties.getExpirationMs(),
            new RegisterV2Response.UserInfo(
                String.valueOf(user.getId()),
                user.getDisplayNickname(),
                user.getName(),
                user.getEmail(),
                user.getUserType(),
                user.getLevel(),
                user.isOnboardingCompleted()
            )
        );
        return new RegisterV2Result(tokenPair.refreshToken(), response);
    }
}

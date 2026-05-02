package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ConflictException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.response.ProfileResponse;
import com.ssac.ssacbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 조회 및 닉네임 수정 비즈니스 로직.
 *
 * <p>변경 기준: docs/conventions.md#트랜잭션-규칙
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    /**
     * 현재 로그인한 사용자의 프로필을 조회한다.
     *
     * @param email JWT에서 추출한 사용자 이메일
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        log.debug("프로필 조회: email={}", email);
        User user = findUserByEmail(email);
        return ProfileResponse.from(user);
    }

    /**
     * 닉네임을 수정한다. 중복 닉네임이면 409 Conflict를 반환한다.
     *
     * @param email    JWT에서 추출한 사용자 이메일
     * @param nickname 변경할 닉네임 (유효성 검사는 Controller 레이어에서 수행)
     */
    @Transactional
    public ProfileResponse updateNickname(String email, String nickname) {
        log.debug("닉네임 수정 요청: email={}, nickname={}", email, nickname);
        User user = findUserByEmail(email);

        if (!user.getNickname().equals(nickname) && userRepository.existsByNickname(nickname)) {
            throw new ConflictException(ErrorCode.NICKNAME_DUPLICATED);
        }

        user.updateNickname(nickname);
        return ProfileResponse.from(user);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}

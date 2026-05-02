package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.response.UserSummaryResponse;
import com.ssac.ssacbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 전용 사용자 관리 비즈니스 로직.
 *
 * <p>사용자 목록 조회 및 권한 변경을 제공한다.
 * 권한 변경은 DB에 즉시 반영되며, JwtAuthenticationFilter의 DB 조회로 다음 요청부터 적용된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    /**
     * 전체 사용자 목록을 페이지네이션으로 조회한다.
     */
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserSummaryResponse::from);
    }

    /**
     * 특정 사용자의 권한을 변경한다.
     *
     * <p>변경된 권한은 DB에 즉시 반영된다. 해당 사용자의 다음 API 요청부터 새 권한이 적용된다.
     * GUEST 역할로의 변경은 허용하지 않는다.
     *
     * @param userId  대상 사용자 ID
     * @param newRole 변경할 권한
     */
    @Transactional
    public UserSummaryResponse updateUserRole(Long userId, UserRole newRole) {
        if (newRole == UserRole.GUEST) {
            throw new BadRequestException(ErrorCode.ROLE_ASSIGNMENT_INVALID);
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        user.updateRole(newRole);
        log.info("사용자 권한 변경: userId={}, newRole={}", userId, newRole);
        return UserSummaryResponse.from(user);
    }
}

package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.auth.AdminCode;
import com.ssac.ssacbackend.domain.feedback.FeedbackStatus;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.response.AdminCodeCreateResponse;
import com.ssac.ssacbackend.dto.response.AdminHomeResponse;
import com.ssac.ssacbackend.dto.response.UserSummaryResponse;
import com.ssac.ssacbackend.repository.AdminCodeRepository;
import com.ssac.ssacbackend.repository.FeedbackRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 전용 사용자 관리 및 홈 대시보드 비즈니스 로직.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final AdminCodeRepository adminCodeRepository;

    /**
     * 관리자 홈 화면 데이터를 조회한다.
     *
     * @param adminEmail 관리자 이메일 (SecurityContext principal)
     */
    @Transactional(readOnly = true)
    public AdminHomeResponse getAdminHome(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        long totalUsers = userRepository.countByRoleNot(UserRole.GUEST);
        long totalFeedbacks = feedbackRepository.count();
        long pendingFeedbacks = feedbackRepository.countByStatus(FeedbackStatus.PENDING);

        return new AdminHomeResponse(
            new AdminHomeResponse.AdminInfo(admin.getDisplayNickname(), admin.getRole().name()),
            new AdminHomeResponse.Stats(totalUsers, totalFeedbacks, pendingFeedbacks)
        );
    }

    /**
     * 관리자 코드를 발급한다.
     *
     * <p>UUID로 원문 코드를 생성하고 SHA-256 해시로 저장한다.
     * 원문은 이 응답에서 단 한 번만 반환된다.
     *
     * @param adminUserId 코드와 연결할 관리자 사용자 ID (ADMIN 역할이어야 함)
     * @param expiresAt   만료 일시 (null이면 무기한)
     */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Transactional
    public AdminCodeCreateResponse createAdminCode(Long adminUserId, OffsetDateTime expiresAt) {
        User targetUser = userRepository.findById(adminUserId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        if (targetUser.getRole() != UserRole.ADMIN) {
            throw new BadRequestException(ErrorCode.ROLE_ASSIGNMENT_INVALID);
        }

        LocalDateTime expiresAtKst = expiresAt != null
            ? expiresAt.atZoneSameInstant(KST).toLocalDateTime()
            : null;

        String rawCode = UUID.randomUUID().toString();
        String codeHash = AdminLoginService.sha256(rawCode);

        AdminCode adminCode = AdminCode.builder()
            .codeHash(codeHash)
            .adminUserId(adminUserId)
            .expiresAt(expiresAtKst)
            .build();

        AdminCode saved = adminCodeRepository.save(adminCode);
        log.info("관리자 코드 발급: adminUserId={}, codeId={}", adminUserId, saved.getId());

        return AdminCodeCreateResponse.of(
            String.valueOf(saved.getId()),
            rawCode,
            adminUserId,
            expiresAtKst,
            saved.getCreatedAt()
        );
    }

    /**
     * 전체 사용자 목록을 페이지네이션으로 조회한다.
     */
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserSummaryResponse::from);
    }

    /**
     * 특정 사용자의 권한을 변경한다.
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

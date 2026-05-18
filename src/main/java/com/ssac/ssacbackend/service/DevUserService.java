package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ErrorCode;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발/로컬 환경 전용 사용자 관리 서비스.
 *
 * <p>테스트 초기화 목적으로 사용자 및 모든 연관 데이터를 삭제한다.
 * prod 프로파일에서는 Bean 자체가 생성되지 않는다.
 */
@Slf4j
@Service
@Profile("!prod")
@RequiredArgsConstructor
public class DevUserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final NotificationRepository notificationRepository;
    private final ContentProgressRepository contentProgressRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MigrationFailureRepository migrationFailureRepository;
    private final UserInterestRepository userInterestRepository;

    /**
     * 이메일로 사용자와 모든 연관 데이터를 삭제한다.
     *
     * <p>삭제 순서 (FK 제약 준수):
     * <ol>
     *   <li>SocialAccount (user_id NOT NULL FK)</li>
     *   <li>QuizAttempt → AttemptAnswer (CascadeType.ALL 자동 cascade)</li>
     *   <li>Notification (user_id NOT NULL FK)</li>
     *   <li>ContentProgress (user_id NOT NULL FK)</li>
     *   <li>RefreshToken (userId 컬럼, 제약 없음)</li>
     *   <li>MigrationFailure (userId 컬럼, 제약 없음)</li>
     *   <li>UserInterest (user_id FK)</li>
     *   <li>User</li>
     * </ol>
     *
     * @param email 삭제할 사용자의 이메일
     * @throws NotFoundException 해당 이메일의 사용자가 없을 때
     */
    @Transactional
    public void deleteByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        Long userId = user.getId();
        socialAccountRepository.deleteByUser(user);
        quizAttemptRepository.deleteByUser(user);
        notificationRepository.deleteByUserEmail(email);
        contentProgressRepository.deleteByUserEmail(email);
        refreshTokenRepository.deleteByUserId(userId);
        migrationFailureRepository.deleteByUserId(userId);
        userInterestRepository.deleteByUserId(userId);
        userRepository.delete(user);

        log.info("[DEV] 사용자 삭제 완료: email={}, userId={}", email, userId);
    }
}

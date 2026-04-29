package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.domain.user.MigrationFailure;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.repository.MigrationFailureRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guest 사용자의 데이터를 로그인 후 회원 계정으로 이전한다.
 *
 * <p>OAuth2/Naver 로그인 성공 시 guestId 쿠키가 존재하면 호출된다.
 * QuizAttempt의 guestId를 userId로 교체하여 기록을 유지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestMigrationService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final MigrationFailureRepository migrationFailureRepository;

    @Transactional
    public void migrateGuestData(String guestId, User user) {
        log.debug("Guest 마이그레이션 시작: guestId={}, userId={}", guestId, user.getId());
        List<QuizAttempt> guestAttempts = quizAttemptRepository.findByGuestId(guestId);
        if (guestAttempts.isEmpty()) {
            log.debug("마이그레이션 대상 없음: guestId={}", guestId);
            return;
        }
        try {
            guestAttempts.forEach(attempt -> attempt.transferToUser(user));
            log.info("Guest 데이터 마이그레이션 완료: guestId={}, userId={}, 건수={}",
                guestId, user.getId(), guestAttempts.size());
        } catch (Exception e) {
            log.error("Guest 데이터 마이그레이션 실패: guestId={}, userId={}, 원인={}",
                guestId, user.getId(), e.getMessage(), e);
            recordMigrationFailure(guestId, user.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 마이그레이션 실패를 별도 트랜잭션으로 기록한다.
     * 부모 트랜잭션이 롤백되어도 실패 기록은 남아야 하므로 REQUIRES_NEW 사용.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMigrationFailure(String guestId, Long userId, String message) {
        try {
            MigrationFailure failure = MigrationFailure.builder()
                .guestId(guestId)
                .userId(userId)
                .errorMessage(message)
                .build();
            migrationFailureRepository.save(failure);
            log.info("마이그레이션 실패 기록 저장 완료: guestId={}", guestId);
        } catch (Exception e) {
            log.error("마이그레이션 실패 기록 저장 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}

package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
            throw e;
        }
    }
}

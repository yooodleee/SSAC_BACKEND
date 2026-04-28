package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guest 사용자의 데이터를 로그인 후 회원 계정으로 이전한다.
 *
 * <p>OAuth2/Naver 로그인 성공 시 guestId 쿠키가 존재하면 호출된다.
 * 동일 퀴즈에 중복 기록이 있으면 최신 기록을 우선 유지하고 나머지는 삭제한다.
 * 마이그레이션 실패 시 예외를 전파하지 않아 로그인 흐름이 유지된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestMigrationService {

    private final QuizAttemptRepository quizAttemptRepository;

    /**
     * Guest 퀴즈 기록을 회원 계정으로 이전한다.
     *
     * @return 마이그레이션 성공 여부 (실패 시에도 로그인은 계속 진행됨)
     */
    @Transactional
    public boolean migrateGuestData(String guestId, User user) {
        log.debug("Guest 마이그레이션 시작: guestId={}, userId={}", guestId, user.getId());
        List<QuizAttempt> guestAttempts = quizAttemptRepository.findByGuestIdWithQuiz(guestId);
        if (guestAttempts.isEmpty()) {
            log.debug("마이그레이션 대상 없음: guestId={}", guestId);
            return true;
        }
        try {
            List<Long> guestQuizIds = guestAttempts.stream()
                .map(qa -> qa.getQuiz().getId())
                .distinct()
                .toList();

            List<QuizAttempt> userExistingAttempts =
                quizAttemptRepository.findByUserAndQuizIds(user, guestQuizIds);

            // 사용자의 기존 기록 중 퀴즈별 최신 기록만 보관
            Map<Long, QuizAttempt> latestUserAttemptByQuiz = userExistingAttempts.stream()
                .collect(Collectors.toMap(
                    qa -> qa.getQuiz().getId(),
                    qa -> qa,
                    (a, b) -> a.getAttemptedAt().isAfter(b.getAttemptedAt()) ? a : b
                ));

            List<QuizAttempt> toDelete = new ArrayList<>();
            int transferCount = 0;

            for (QuizAttempt guestAttempt : guestAttempts) {
                Long quizId = guestAttempt.getQuiz().getId();
                QuizAttempt userAttempt = latestUserAttemptByQuiz.get(quizId);

                if (userAttempt == null) {
                    // 중복 없음: 기록을 회원 계정으로 이전
                    guestAttempt.transferToUser(user);
                    transferCount++;
                } else if (guestAttempt.getAttemptedAt().isAfter(userAttempt.getAttemptedAt())) {
                    // Guest 기록이 더 최신: Guest 기록을 이전하고 기존 회원 기록 삭제
                    guestAttempt.transferToUser(user);
                    toDelete.add(userAttempt);
                    transferCount++;
                } else {
                    // 회원 기록이 더 최신: Guest 기록 삭제
                    toDelete.add(guestAttempt);
                }
            }

            if (!toDelete.isEmpty()) {
                quizAttemptRepository.deleteAll(toDelete);
            }

            log.info("Guest 데이터 마이그레이션 완료: guestId={}, userId={}, 이전={}, 삭제(중복)={}",
                guestId, user.getId(), transferCount, toDelete.size());
            return true;
        } catch (Exception e) {
            log.error("Guest 데이터 마이그레이션 실패: guestId={}, userId={}, 원인={}",
                guestId, user.getId(), e.getMessage(), e);
            return false;
        }
    }
}

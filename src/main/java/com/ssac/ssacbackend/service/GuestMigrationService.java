package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.domain.user.MigrationFailure;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.repository.MigrationFailureRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guest 사용자의 데이터를 로그인 후 회원 계정으로 이전한다.
 *
 * <p>OAuth2/Naver 로그인 성공 시 guestId 쿠키가 존재하면 호출된다.
 * 동일 퀴즈에 중복 기록이 있으면 최신 기록을 우선 유지하고 나머지는 삭제한다.
 * 마이그레이션 실패 시 실패 내역을 별도 테이블에 기록하고 예외를 전파하지 않아 로그인 흐름이 유지된다.
 *
 * <p>{@link MigrationResult} 레코드를 반환하여 이전된 퀴즈 수를 호출자가 응답에 포함할 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestMigrationService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final MigrationFailureRepository migrationFailureRepository;

    /**
     * Guest 마이그레이션 결과를 담는 값 객체.
     *
     * @param success    마이그레이션 성공 여부
     * @param quizCount  이전된 퀴즈 시도 수
     */
    public record MigrationResult(boolean success, int quizCount) {
        public static MigrationResult failure() {
            return new MigrationResult(false, 0);
        }
    }

    /**
     * Guest 퀴즈 기록을 회원 계정으로 이전한다.
     *
     * @return 마이그레이션 결과 (실패 시에도 로그인은 계속 진행됨)
     */
    @Transactional
    public MigrationResult migrateGuestData(String guestId, User user) {
        log.debug("Guest 마이그레이션 시작: guestId={}, userId={}", guestId, user.getId());
        try {
            List<QuizAttempt> guestAttempts = quizAttemptRepository.findByGuestIdWithQuiz(guestId);
            if (guestAttempts.isEmpty()) {
                log.debug("마이그레이션 대상 없음: guestId={}", guestId);
                return new MigrationResult(true, 0);
            }
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
            return new MigrationResult(true, transferCount);
        } catch (Exception e) {
            log.error("Guest 데이터 마이그레이션 실패: guestId={}, userId={}, 원인={}",
                guestId, user.getId(), e.getMessage(), e);
            recordMigrationFailure(guestId, user.getId(), e.getMessage());
            return MigrationResult.failure();
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

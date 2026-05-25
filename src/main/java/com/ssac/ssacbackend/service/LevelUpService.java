package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.domain.user.LevelHistory;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.content.ContentDifficulty;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.LevelUpProgressDto;
import com.ssac.ssacbackend.dto.response.LevelUpResult;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.LevelHistoryRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 레벨업 자동 판정 서비스.
 *
 * <p>콘텐츠 완료 또는 퀴즈 제출 후 호출되며, 조건 충족 시 레벨을 갱신하고 이력을 저장한다.
 *
 * <p>레벨업 조건:
 * <ul>
 *   <li>SEED → SPROUT: 현재 레벨 콘텐츠 70% 완료 + 최근 10문제 정답률 70% 이상</li>
 *   <li>SPROUT → TREE: 현재 레벨 콘텐츠 70% 완료 + 최근 10문제 정답률 80% 이상</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LevelUpService {

    private static final int CONTENT_THRESHOLD = 70;
    private static final int SEED_QUIZ_THRESHOLD = 70;
    private static final int SPROUT_QUIZ_THRESHOLD = 80;
    private static final int RECENT_QUIZ_COUNT = 10;

    private final ContentRepository contentRepository;
    private final ContentProgressRepository contentProgressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final LevelHistoryRepository levelHistoryRepository;

    /**
     * 레벨업 조건을 검증하고 충족 시 레벨을 갱신한다.
     *
     * @param user  대상 사용자 (영속 상태)
     * @param email 사용자 이메일
     * @return 레벨업 판정 결과
     */
    public LevelUpResult checkAndApplyLevelUp(User user, String email) {
        UserLevel currentLevel = user.getLevel();

        if (currentLevel == null || currentLevel == UserLevel.TREE) {
            return LevelUpResult.noLevelUp(currentLevel, buildProgress(currentLevel, 0, 0));
        }

        int contentRate = calculateContentCompletionRate(currentLevel, email);
        int quizRate = calculateRecentQuizCorrectRate(email);
        int quizRequired = currentLevel == UserLevel.SEED ? SEED_QUIZ_THRESHOLD : SPROUT_QUIZ_THRESHOLD;

        LevelUpProgressDto progress = buildProgress(currentLevel, contentRate, quizRate);

        if (contentRate >= CONTENT_THRESHOLD && quizRate >= quizRequired) {
            UserLevel nextLevel = currentLevel == UserLevel.SEED ? UserLevel.SPROUT : UserLevel.TREE;
            user.updateLevel(nextLevel);
            levelHistoryRepository.save(
                LevelHistory.builder()
                    .user(user)
                    .previousLevel(currentLevel)
                    .newLevel(nextLevel)
                    .build()
            );
            log.info("레벨업: email={}, {} → {}", email, currentLevel, nextLevel);
            return LevelUpResult.levelUp(currentLevel, nextLevel);
        }

        return LevelUpResult.noLevelUp(currentLevel, progress);
    }

    private int calculateContentCompletionRate(UserLevel level, String email) {
        ContentDifficulty diffEnum = ContentDifficulty.valueOf(level.name());
        long total = contentRepository.countByDifficulty(diffEnum);
        if (total == 0) {
            return 0;
        }
        long completed = contentProgressRepository.countCompletedByUserEmailAndDifficulty(email, diffEnum);
        return (int) Math.round((double) completed / total * 100);
    }

    private int calculateRecentQuizCorrectRate(String email) {
        List<QuizAttempt> recent = quizAttemptRepository.findRecentByUserEmail(
            email, PageRequest.of(0, RECENT_QUIZ_COUNT));
        if (recent.isEmpty()) {
            return 0;
        }
        long totalQuestions = recent.stream()
            .mapToLong(qa -> qa.getQuiz().getTotalQuestions())
            .sum();
        long totalCorrect = recent.stream()
            .mapToLong(QuizAttempt::getCorrectCount)
            .sum();
        return totalQuestions > 0 ? (int) Math.round((double) totalCorrect / totalQuestions * 100) : 0;
    }

    private LevelUpProgressDto buildProgress(UserLevel level, int contentRate, int quizRate) {
        int quizRequired = (level == UserLevel.SPROUT) ? SPROUT_QUIZ_THRESHOLD : SEED_QUIZ_THRESHOLD;
        return new LevelUpProgressDto(contentRate, quizRate, CONTENT_THRESHOLD, quizRequired);
    }
}

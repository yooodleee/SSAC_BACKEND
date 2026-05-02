package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.dto.response.RecommendationResponse;
import com.ssac.ssacbackend.dto.response.RecommendationResponse.RecommendedQuizResponse;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.QuizRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인화 콘텐츠 추천 비즈니스 로직.
 *
 * <p>추천 전략:
 * <ul>
 *   <li>신규 사용자(응시 기록 없음): 최신 퀴즈 {@value #DEFAULT_LIMIT}개를 기본 추천한다.</li>
 *   <li>기존 사용자: 최근 {@value #ACTIVITY_DAYS}일 내 기록 기반으로
 *       정답률이 {@value #RETRY_THRESHOLD_PCT}% 미만인 퀴즈(RETRY)와
 *       미시도 퀴즈(UNTRIED)를 혼합하여 추천한다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int RETRY_LIMIT = 3;
    private static final int UNTRIED_LIMIT = 3;
    private static final double RETRY_THRESHOLD = 0.7;
    private static final int RETRY_THRESHOLD_PCT = 70;
    private static final int ACTIVITY_DAYS = 30;

    private final UserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;

    /**
     * 로그인한 사용자의 개인화 추천을 반환한다.
     *
     * <p>응시 기록이 전혀 없으면 신규 사용자로 판별하여 기본 추천을 반환한다.
     * 기존 사용자는 최근 {@value #ACTIVITY_DAYS}일 활동을 기준으로 추천한다.
     *
     * @param email JWT에서 추출한 사용자 이메일
     */
    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(String email) {
        log.debug("추천 조회: email={}", email);
        userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        boolean hasAnyAttempt = quizAttemptRepository.existsByUserEmail(email);

        if (!hasAnyAttempt) {
            return buildDefaultRecommendation();
        }

        return buildPersonalizedRecommendation(email);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    private RecommendationResponse buildDefaultRecommendation() {
        List<Quiz> latest = quizRepository.findByOrderByCreatedAtDesc(
            PageRequest.of(0, DEFAULT_LIMIT));
        List<RecommendedQuizResponse> items = latest.stream()
            .map(RecommendedQuizResponse::ofDefault)
            .toList();
        log.debug("신규 사용자 기본 추천: count={}", items.size());
        return new RecommendationResponse(false, items);
    }

    private RecommendationResponse buildPersonalizedRecommendation(String email) {
        LocalDateTime since = LocalDateTime.now().minusDays(ACTIVITY_DAYS);
        List<QuizAttempt> recentAttempts =
            quizAttemptRepository.findByUserEmailSinceWithQuiz(email, since);

        List<RecommendedQuizResponse> recommendations = new ArrayList<>();
        Set<Long> addedQuizIds = new HashSet<>();

        // RETRY: 최근 정답률이 낮은 퀴즈를 정답률 오름차순으로 추가
        recentAttempts.stream()
            .filter(qa -> qa.getQuiz().getTotalQuestions() > 0)
            .filter(qa -> accuracyOf(qa) < RETRY_THRESHOLD)
            .sorted(Comparator.comparingDouble(this::accuracyOf))
            .filter(qa -> addedQuizIds.add(qa.getQuiz().getId()))
            .limit(RETRY_LIMIT)
            .forEach(qa -> {
                double pct = Math.round(accuracyOf(qa) * 1000.0) / 10.0;
                recommendations.add(RecommendedQuizResponse.ofRetry(qa.getQuiz(), pct));
            });

        // UNTRIED: 아직 한 번도 풀지 않은 최신 퀴즈
        List<Quiz> untried = quizRepository.findUntriedByUserEmail(
            email, PageRequest.of(0, UNTRIED_LIMIT));
        untried.stream()
            .filter(q -> addedQuizIds.add(q.getId()))
            .forEach(q -> recommendations.add(RecommendedQuizResponse.ofUntried(q)));

        log.debug("개인화 추천: email={}, retryCount={}, untriedCount={}",
            email,
            recommendations.stream().filter(r ->
                r.type() == RecommendationResponse.RecommendationType.RETRY).count(),
            recommendations.stream().filter(r ->
                r.type() == RecommendationResponse.RecommendationType.UNTRIED).count());

        return new RecommendationResponse(true, recommendations);
    }

    private double accuracyOf(QuizAttempt qa) {
        int total = qa.getQuiz().getTotalQuestions();
        return total > 0 ? (double) qa.getCorrectCount() / total : 0.0;
    }
}

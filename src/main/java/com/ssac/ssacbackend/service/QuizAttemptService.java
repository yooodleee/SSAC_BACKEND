package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.quiz.AttemptAnswer;
import com.ssac.ssacbackend.domain.quiz.Question;
import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.request.AttemptSortType;
import com.ssac.ssacbackend.dto.request.QuizSubmitRequest;
import com.ssac.ssacbackend.dto.request.StatPeriod;
import com.ssac.ssacbackend.dto.response.PeriodStatResponse;
import com.ssac.ssacbackend.dto.response.QuizAttemptDetailResponse;
import com.ssac.ssacbackend.dto.response.QuizAttemptSummaryResponse;
import com.ssac.ssacbackend.dto.response.UserStatsResponse;
import com.ssac.ssacbackend.repository.QuestionRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.QuizRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀴즈 응시 기록 및 통계 비즈니스 로직.
 *
 * <p>퀴즈 제출 시 서버에서 정답을 검증하고 결과를 저장한다.
 * 기록 조회는 N+1 방지를 위해 JOIN FETCH 쿼리를 활용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    /**
     * 퀴즈를 제출하고 응시 기록을 저장한다.
     *
     * @param email   JWT에서 추출한 사용자 이메일
     * @param request 제출 요청 (퀴즈 ID, 문항별 선택 답안)
     */
    @Transactional
    public QuizAttemptSummaryResponse submitQuiz(String email, QuizSubmitRequest request) {
        log.debug("퀴즈 제출: email={}, quizId={}", email, request.quizId());
        User user = findUserByEmail(email);
        QuizAttempt attempt = createAttempt(user, null, request);
        log.info("퀴즈 제출 완료: email={}, quizId={}, score={}/{}",
            email, request.quizId(), attempt.getEarnedScore(), attempt.getQuiz().getMaxScore());
        return QuizAttemptSummaryResponse.from(attempt);
    }

    /**
     * 비회원(Guest)의 퀴즈 응시 기록을 guestId로 저장한다. 로그인 시 회원 기록으로 전환된다.
     *
     * @param guestId JWT sub에서 추출한 임시 사용자 ID(UUID)
     * @param request 제출 요청
     */
    @Transactional
    public QuizAttemptSummaryResponse submitQuizAsGuest(String guestId, QuizSubmitRequest request) {
        log.debug("Guest 퀴즈 제출: guestId={}, quizId={}", guestId, request.quizId());
        QuizAttempt attempt = createAttempt(null, guestId, request);
        log.info("Guest 퀴즈 제출 완료: guestId={}, quizId={}, score={}/{}",
            guestId, request.quizId(), attempt.getEarnedScore(), attempt.getQuiz().getMaxScore());
        return QuizAttemptSummaryResponse.from(attempt);
    }

    private QuizAttempt createAttempt(User user, String guestId, QuizSubmitRequest request) {
        Quiz quiz = quizRepository.findById(request.quizId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.QUIZ_NOT_FOUND));

        List<Question> questions = questionRepository.findByQuizIdOrderByQuestionOrder(quiz.getId());
        Map<Long, Question> questionMap = questions.stream()
            .collect(Collectors.toMap(Question::getId, q -> q));

        int earnedScore = 0;
        int correctCount = 0;
        for (QuizSubmitRequest.AnswerItem item : request.answers()) {
            Question question = questionMap.get(item.questionId());
            if (question == null) {
                throw new BadRequestException(ErrorCode.QUIZ_QUESTION_MISMATCH, "퀴즈에 속하지 않는 문항입니다: questionId=" + item.questionId());
            }
            if (question.getCorrectAnswer().equals(item.selectedAnswer())) {
                earnedScore += question.getPoints();
                correctCount++;
            }
        }

        QuizAttempt attempt = QuizAttempt.builder()
            .user(user)
            .guestId(guestId)
            .quiz(quiz)
            .earnedScore(earnedScore)
            .correctCount(correctCount)
            .build();
        quizAttemptRepository.save(attempt);

        for (QuizSubmitRequest.AnswerItem item : request.answers()) {
            Question question = questionMap.get(item.questionId());
            boolean correct = question.getCorrectAnswer().equals(item.selectedAnswer());
            AttemptAnswer answer = AttemptAnswer.builder()
                .quizAttempt(attempt)
                .question(question)
                .selectedAnswer(item.selectedAnswer())
                .correct(correct)
                .earnedPoints(correct ? question.getPoints() : 0)
                .build();
            attempt.addAnswer(answer);
        }
        return attempt;
    }

    /**
     * 비회원(Guest)의 퀴즈 응시 기록 목록을 guestId 기준으로 조회한다.
     *
     * @param guestId JWT sub에서 추출한 임시 사용자 ID(UUID)
     */
    @Transactional(readOnly = true)
    public Page<QuizAttemptSummaryResponse> getGuestHistory(
        String guestId, int page, int size, AttemptSortType sortType) {
        log.debug("Guest 퀴즈 기록 조회: guestId={}, page={}, size={}, sort={}", guestId, page, size, sortType);

        Sort sort = buildSort(sortType);
        Pageable pageable = PageRequest.of(page, size, sort);

        return quizAttemptRepository
            .findByGuestIdPagedWithQuiz(guestId, pageable)
            .map(QuizAttemptSummaryResponse::from);
    }

    /**
     * 사용자의 퀴즈 응시 기록 목록을 페이지네이션과 정렬 옵션으로 조회한다.
     *
     * <p>JOIN FETCH로 quiz를 함께 로드하여 N+1 문제를 방지한다.
     *
     * @param email    JWT에서 추출한 사용자 이메일
     * @param page     페이지 번호 (0부터 시작)
     * @param size     페이지 크기
     * @param sortType 정렬 기준
     * @return 페이지네이션된 응시 기록 목록
     */
    @Transactional(readOnly = true)
    public Page<QuizAttemptSummaryResponse> getHistory(
        String email, int page, int size, AttemptSortType sortType) {
        log.debug("퀴즈 기록 조회: email={}, page={}, size={}, sort={}", email, page, size, sortType);

        Sort sort = buildSort(sortType);
        Pageable pageable = PageRequest.of(page, size, sort);

        return quizAttemptRepository
            .findByUserEmailFetchQuiz(email, pageable)
            .map(QuizAttemptSummaryResponse::from);
    }

    /**
     * 퀴즈 응시 상세 결과를 조회한다.
     *
     * <p>JOIN FETCH로 answers와 question을 함께 로드하여 N+1 문제를 방지한다.
     * 본인의 기록만 조회할 수 있다.
     *
     * @param email     JWT에서 추출한 사용자 이메일
     * @param attemptId 응시 기록 ID
     * @return 문항별 답안 상세 포함 응시 결과
     */
    @Transactional(readOnly = true)
    public QuizAttemptDetailResponse getDetail(String email, Long attemptId) {
        log.debug("퀴즈 상세 조회: email={}, attemptId={}", email, attemptId);

        QuizAttempt attempt = quizAttemptRepository
            .findDetailByIdAndUserEmail(attemptId, email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND));

        return QuizAttemptDetailResponse.from(attempt);
    }

    /**
     * 사용자의 누적 통계와 기간별 통계를 조회한다.
     *
     * <p>누적 통계는 집계 쿼리로, 기간별 통계는 Java 스트림 그룹화로 계산한다.
     * 스트림 그룹화 방식은 MySQL/H2 모두 호환된다.
     *
     * @param email  JWT에서 추출한 사용자 이메일
     * @param period 기간 단위 (DAILY/WEEKLY/MONTHLY)
     * @return 누적 통계 + 기간별 통계
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getStats(String email, StatPeriod period) {
        log.debug("퀴즈 통계 조회: email={}, period={}", email, period);

        List<Object[]> overallRows = quizAttemptRepository.aggregateOverallStats(email);
        if (overallRows.isEmpty() || overallRows.get(0)[0] == null) {
            return UserStatsResponse.empty();
        }

        Object[] row = overallRows.get(0);
        long totalScore = toLong(row[0]);
        long totalAttempts = toLong(row[1]);
        long totalCorrect = toLong(row[2]);
        long totalQuestions = toLong(row[3]);
        double averageScore = totalAttempts > 0 ? (double) totalScore / totalAttempts : 0.0;
        double accuracyRate = totalQuestions > 0
            ? Math.round((double) totalCorrect / totalQuestions * 1000.0) / 10.0 : 0.0;

        List<QuizAttempt> periodAttempts =
            quizAttemptRepository.findByUserEmailSinceWithQuiz(email, period.since());
        List<PeriodStatResponse> periodStats = buildPeriodStats(periodAttempts, period);

        return new UserStatsResponse(
            totalScore, totalAttempts,
            Math.round(averageScore * 10.0) / 10.0,
            totalCorrect, totalQuestions, accuracyRate,
            periodStats
        );
    }

    // ── 내부 유틸리티 ────────────────────────────────────────────────────────

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private Sort buildSort(AttemptSortType sortType) {
        if (sortType == AttemptSortType.SCORE) {
            return Sort.by(Sort.Direction.DESC, "earnedScore")
                .and(Sort.by(Sort.Direction.DESC, "attemptedAt"));
        }
        return Sort.by(Sort.Direction.DESC, "attemptedAt");
    }

    private List<PeriodStatResponse> buildPeriodStats(
        List<QuizAttempt> attempts, StatPeriod period) {

        if (attempts.isEmpty()) {
            return List.of();
        }

        Map<String, List<QuizAttempt>> grouped = groupByPeriod(attempts, period);

        return grouped.entrySet().stream()
            .sorted(Map.Entry.<String, List<QuizAttempt>>comparingByKey().reversed())
            .map(entry -> toPeriodStatResponse(entry.getKey(), entry.getValue()))
            .toList();
    }

    private Map<String, List<QuizAttempt>> groupByPeriod(
        List<QuizAttempt> attempts, StatPeriod period) {

        return attempts.stream().collect(
            Collectors.groupingBy(
                qa -> periodLabel(qa.getAttemptedAt().toLocalDate(), period),
                TreeMap::new,
                Collectors.toList()
            )
        );
    }

    private String periodLabel(LocalDate date, StatPeriod period) {
        return switch (period) {
            case DAILY -> date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case WEEKLY -> {
                int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
                yield date.getYear() + "-W" + String.format("%02d", week);
            }
            case MONTHLY -> YearMonth.from(date).toString();
        };
    }

    private PeriodStatResponse toPeriodStatResponse(
        String label, List<QuizAttempt> attempts) {

        int attemptCount = attempts.size();
        int totalScore = attempts.stream().mapToInt(QuizAttempt::getEarnedScore).sum();
        double avgScore = attemptCount > 0
            ? Math.round((double) totalScore / attemptCount * 10.0) / 10.0 : 0.0;

        long correct = attempts.stream().mapToLong(QuizAttempt::getCorrectCount).sum();
        long total = attempts.stream()
            .mapToLong(qa -> qa.getQuiz().getTotalQuestions()).sum();
        double accuracy = total > 0
            ? Math.round((double) correct / total * 1000.0) / 10.0 : 0.0;

        return new PeriodStatResponse(label, attemptCount, totalScore, avgScore, accuracy);
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}

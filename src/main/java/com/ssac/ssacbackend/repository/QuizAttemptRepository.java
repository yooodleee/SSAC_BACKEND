package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.quiz.QuizAttempt;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 퀴즈 응시 기록 데이터 접근 인터페이스.
 *
 * <p>N+1 문제 방지 전략:
 * <ul>
 *   <li>목록 조회: ManyToOne(quiz) JOIN FETCH + 별도 countQuery로 페이지네이션 안전하게 처리</li>
 *   <li>상세 조회: OneToMany(answers) + 중첩 ManyToOne(question) JOIN FETCH (페이지네이션 없음)</li>
 *   <li>통계 조회: 집계 함수로 단일 쿼리 처리, Object[] 반환으로 ArchUnit 레이어 준수</li>
 * </ul>
 */
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /**
     * 사용자의 응시 기록 목록을 quiz 정보와 함께 조회한다 (N+1 방지).
     *
     * <p>ManyToOne JOIN FETCH는 페이지네이션과 함께 사용해도 LIMIT이 DB에서 처리되어 안전하다.
     * 별도 countQuery로 COUNT(*) 쿼리를 최적화한다.
     *
     * @param email    사용자 이메일
     * @param pageable 페이지 정보 및 정렬 조건
     */
    @Query(
        value = "SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz"
            + " WHERE qa.user.email = :email",
        countQuery = "SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.user.email = :email"
    )
    Page<QuizAttempt> findByUserEmailFetchQuiz(
        @Param("email") String email, Pageable pageable);

    /**
     * 응시 기록 상세 조회 (answers → question 전체 JOIN FETCH).
     *
     * <p>DISTINCT로 OneToMany JOIN FETCH 시 발생하는 카테시안 곱 중복을 제거한다.
     *
     * @param id    응시 기록 ID
     * @param email 접근 제어를 위한 사용자 이메일
     */
    @Query("""
        SELECT DISTINCT qa FROM QuizAttempt qa
        JOIN FETCH qa.quiz
        JOIN FETCH qa.answers aa
        JOIN FETCH aa.question
        WHERE qa.id = :id AND qa.user.email = :email
        """)
    Optional<QuizAttempt> findDetailByIdAndUserEmail(
        @Param("id") Long id, @Param("email") String email);

    /**
     * 특정 기간 이후의 응시 기록을 quiz 정보와 함께 조회한다.
     *
     * <p>기간별 통계 집계에 사용하며, Java 스트림으로 그룹화하여 MySQL/H2 호환성을 유지한다.
     *
     * @param email 사용자 이메일
     * @param since 조회 시작 시각 (이 시각 이후의 기록만 반환)
     */
    @Query("""
        SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz
        WHERE qa.user.email = :email AND qa.attemptedAt >= :since
        ORDER BY qa.attemptedAt DESC
        """)
    List<QuizAttempt> findByUserEmailSinceWithQuiz(
        @Param("email") String email, @Param("since") LocalDateTime since);

    /**
     * 사용자의 전체 누적 통계를 단일 집계 쿼리로 조회한다.
     *
     * <p>반환 배열 구조: [totalScore, totalAttempts, totalCorrect, totalQuestions]
     * ArchUnit(Repository는 Domain/Common만 접근 가능) 준수를 위해 Object[]를 반환한다.
     *
     * @param email 사용자 이메일
     */
    @Query("""
        SELECT SUM(qa.earnedScore), COUNT(qa),
               SUM(qa.correctCount), SUM(q.totalQuestions)
        FROM QuizAttempt qa JOIN qa.quiz q
        WHERE qa.user.email = :email
        """)
    List<Object[]> aggregateOverallStats(@Param("email") String email);
}

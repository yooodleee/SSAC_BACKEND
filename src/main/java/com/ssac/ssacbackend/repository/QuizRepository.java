package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.quiz.Quiz;
import com.ssac.ssacbackend.domain.user.UserLevel;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 퀴즈 데이터 접근 인터페이스.
 */
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /**
     * 최신 퀴즈를 생성일시 내림차순으로 조회한다. 신규 사용자 기본 추천에 사용한다.
     *
     * @param pageable 페이지 크기로 개수를 제한한다
     */
    List<Quiz> findByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 사용자가 한 번도 응시하지 않은 퀴즈를 최신 순으로 조회한다.
     *
     * <p>NOT EXISTS 서브쿼리를 사용하여 시도하지 않은 퀴즈를 필터링한다.
     *
     * @param email    사용자 이메일
     * @param pageable 페이지 크기로 개수를 제한한다
     */
    @Query("""
        SELECT q FROM Quiz q
        WHERE NOT EXISTS (
            SELECT qa FROM QuizAttempt qa
            WHERE qa.quiz.id = q.id AND qa.user.email = :email
        )
        ORDER BY q.createdAt DESC
        """)
    List<Quiz> findUntriedByUserEmail(@Param("email") String email, Pageable pageable);

    /**
     * 사용자가 완전히 풀지 못한 난이도 일치 퀴즈 목록 (todayQuiz 후보).
     *
     * <p>earnedScore = maxScore인 완전 정답 기록이 없는 퀴즈만 반환한다.
     */
    @Query("""
        SELECT q FROM Quiz q
        WHERE q.difficulty = :difficulty
          AND NOT EXISTS (
            SELECT qa FROM QuizAttempt qa
            WHERE qa.quiz = q AND qa.user.email = :email
              AND qa.earnedScore = q.maxScore
          )
        ORDER BY q.id ASC
        """)
    List<Quiz> findUncompletedByDifficultyAndUserEmail(
        @Param("difficulty") UserLevel difficulty, @Param("email") String email);
}

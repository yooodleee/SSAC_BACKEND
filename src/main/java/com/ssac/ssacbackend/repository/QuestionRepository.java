package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.quiz.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 퀴즈 문항 데이터 접근 인터페이스.
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * 퀴즈에 속한 모든 문항을 순서대로 조회한다.
     *
     * @param quizId 퀴즈 ID
     */
    List<Question> findByQuizIdOrderByQuestionOrder(Long quizId);
}

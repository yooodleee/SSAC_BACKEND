package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.quiz.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 퀴즈 데이터 접근 인터페이스.
 */
public interface QuizRepository extends JpaRepository<Quiz, Long> {
}

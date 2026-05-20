package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.feedback.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}

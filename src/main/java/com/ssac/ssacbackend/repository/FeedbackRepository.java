package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.feedback.Feedback;
import com.ssac.ssacbackend.domain.feedback.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Feedback> findByStatusOrderByCreatedAtDesc(FeedbackStatus status, Pageable pageable);

    long countByStatus(FeedbackStatus status);
}

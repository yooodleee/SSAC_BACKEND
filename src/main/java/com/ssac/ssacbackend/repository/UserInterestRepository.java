package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.onboarding.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    void deleteByUserId(Long userId);
}

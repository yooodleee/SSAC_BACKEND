package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.onboarding.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    void deleteByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT ui.domainId FROM UserInterest ui WHERE ui.userId = :userId")
    java.util.List<String> findDomainIdsByUserId(
        @org.springframework.data.repository.query.Param("userId") Long userId);
}

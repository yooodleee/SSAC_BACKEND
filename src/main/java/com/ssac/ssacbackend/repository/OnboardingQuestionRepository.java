package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.onboarding.OnboardingQuestion;
import com.ssac.ssacbackend.domain.user.UserType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingQuestionRepository extends JpaRepository<OnboardingQuestion, Long> {

    List<OnboardingQuestion> findByUserTypeAndIsActiveTrueOrderByQuestionOrderAsc(UserType userType);
}

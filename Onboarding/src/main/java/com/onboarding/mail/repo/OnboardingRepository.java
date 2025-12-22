package com.onboarding.mail.repo;

import com.onboarding.mail.entity.CandidateOnboardingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OnboardingRepository extends JpaRepository<CandidateOnboardingDetails,Long> {
}

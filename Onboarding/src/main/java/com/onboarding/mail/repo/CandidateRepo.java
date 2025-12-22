package com.onboarding.mail.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onboarding.mail.entity.Candidate;

@Repository
public interface CandidateRepo extends JpaRepository<Candidate, Long>{

}

package com.onboarding.mail.repo;

import aj.org.objectweb.asm.commons.Remapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onboarding.mail.entity.Candidate;

import java.util.Optional;

@Repository
public interface CandidateRepo extends JpaRepository<Candidate, Long>{

    Optional<Candidate> findByEmail(String email);

    boolean existsByEmail(String email);
}

package com.example.OfferLetter.Repository;

import com.example.OfferLetter.Entity.JobApplicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicantRepository extends JpaRepository<JobApplicant, Long> {

}


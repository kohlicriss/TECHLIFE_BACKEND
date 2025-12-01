package com.example.OfferLetter.Repository;


import com.example.OfferLetter.Entity.JobDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobDetailsRepository extends JpaRepository<JobDetails, String> {
    List<JobDetails> findByEmployeeEmployeeIdOrderByStartDateDesc(String employeeId);
    List<JobDetails> findByDepartment(String department);
    //List<JobDetails> findByJobRole(String jobRole);
}

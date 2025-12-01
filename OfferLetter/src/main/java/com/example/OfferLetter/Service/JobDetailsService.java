package com.example.OfferLetter.Service;


import com.example.OfferLetter.Entity.JobDetails;
import com.example.OfferLetter.Repository.JobDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobDetailsService {

    @Autowired
    private JobDetailsRepository jobDetailsRepository;

    public JobDetails saveJobDetails(JobDetails jobDetails) {
        return jobDetailsRepository.save(jobDetails);
    }

    public JobDetails getLatestJobDetailsByEmployeeId(String employeeId) {
        List<JobDetails> jobDetails = jobDetailsRepository.findByEmployeeEmployeeIdOrderByStartDateDesc(employeeId);
        return jobDetails.isEmpty() ? null : jobDetails.get(0);
    }

    public List<JobDetails> getJobHistoryByEmployeeId(String employeeId) {
        return jobDetailsRepository.findByEmployeeEmployeeIdOrderByStartDateDesc(employeeId);
    }

    public void deleteJobDetails(String jobId) {
        jobDetailsRepository.deleteById(jobId);
    }
}
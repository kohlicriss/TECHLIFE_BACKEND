package com.example.OfferLetter.Service;

import com.example.OfferLetter.Repository.JobApplicantRepository;
import com.example.OfferLetter.Repository.JobsRepository;
import com.example.OfferLetter.Dto.JobApplicationDto;
import com.example.OfferLetter.Entity.JobApplicant;
import com.example.OfferLetter.Entity.Jobs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class JobsService {

    @Autowired
    private JobsRepository jobsRepository;

    @Autowired
    private JobApplicantRepository jobApplicantRepository;

    public Jobs createJobs(@RequestBody Jobs jobs){
        return jobsRepository.save(jobs);
    }


    public JobApplicant applicantData(JobApplicationDto applicationDto) {
        try {
            JobApplicant jobApplication = new JobApplicant();

            // Set application data
            jobApplication.setStudentName(applicationDto.getStudentName());
            jobApplication.setStdEmail(applicationDto.getStdEmail());
            jobApplication.setPhoneNumber(applicationDto.getPhoneNumber());
            jobApplication.setJobTitle(applicationDto.getJobTitle());

            // Handle file upload
            MultipartFile resumeFile = applicationDto.getResumeFile();
            if (resumeFile != null && !resumeFile.isEmpty()) {
                jobApplication.setResumeFileName(resumeFile.getOriginalFilename());
                jobApplication.setResumeFileType(resumeFile.getContentType());
                jobApplication.setResumeFileSize(resumeFile.getSize());
                jobApplication.setResumeFileData(resumeFile.getBytes());
            }

            return jobApplicantRepository.save(jobApplication); // Use correct repository
        } catch (IOException e) {
            throw new RuntimeException("Failed to process resume file", e);
        }
    }

    public List<Jobs> getAllJobs(){
        return jobsRepository.findAll();
    }


    public List<JobApplicant> getAllApplicants() {
        return jobApplicantRepository.findAll();
    }

    public Jobs editJobDetails(Long id, Jobs jobs){
        Optional<Jobs> existingJob = jobsRepository.findById(id);

        if(existingJob.isPresent()){
            Jobs existing = existingJob.get();

            existing.setJobTitle(jobs.getJobTitle());
            existing.setExperience(jobs.getExperience());
            existing.setJobType(jobs.getJobType());
            existing.setLocation(jobs.getLocation());
            existing.setSkills(jobs.getSkills());
            existing.setDescription(jobs.getDescription());
            existing.setDepartment(jobs.getDepartment());
            existing.setStatus(jobs.getStatus());

            return jobsRepository.save(existing);
        } else {
            throw new RuntimeException("Job Id not found: " + id);
        }
    }

    public void deleteJob(Long id){
        if(!jobsRepository.existsById(id)){
            throw new RuntimeException("Job id not found: " + id);
        }
        jobsRepository.deleteById(id);
    }
}
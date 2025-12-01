package com.example.OfferLetter.Controller;


import com.example.OfferLetter.Repository.JobApplicantRepository;
import com.example.OfferLetter.Repository.JobsRepository;
import com.example.OfferLetter.Service.JobsService;
import com.example.OfferLetter.Dto.JobApplicationDto;
import com.example.OfferLetter.Entity.JobApplicant;
import com.example.OfferLetter.Entity.Jobs;
import com.example.OfferLetter.Handlers.ApplicationResponse;
import com.example.OfferLetter.Handlers.ErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobGet {

    @Autowired
    private JobsService jobsService;

    @Autowired
    private JobApplicantRepository jobApplicantRepository;

    @GetMapping("/getall")
    public List<Jobs> getJobs() {
        return jobsService.getAllJobs();
    }


    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applyForJob(@ModelAttribute JobApplicationDto applicationDto) {
        try {
            JobApplicant savedApplication = jobsService.applicantData(applicationDto);

            return ResponseEntity.ok().body(
                    new ApplicationResponse("Application submitted successfully!")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ErrorException("Failed to submit application: " + e.getMessage())
            );
        }
    }

    @GetMapping("/{applicantId}/resume")
    public ResponseEntity<?> downloadResume(@PathVariable Long applicantId) {
        try {
            JobApplicant applicant = jobApplicantRepository.findById(applicantId)
                    .orElseThrow(() -> new RuntimeException("Applicant not found"));

            if (applicant.getResumeFileData() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorException("Resume not found for applicant"));
            }

            // Determine content type based on file extension
            String contentType = determineContentType(applicant.getResumeFileName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", applicant.getResumeFileName());
            headers.setContentLength(applicant.getResumeFileSize());

            return new ResponseEntity<>(applicant.getResumeFileData(), headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorException("Applicant not found: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorException("Error downloading resume"));
        }
    }


    private String determineContentType(String fileName) {
        if (fileName == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        if (fileName.toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.toLowerCase().endsWith(".doc")) {
            return "application/msword";
        } else if (fileName.toLowerCase().endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }


    @GetMapping("/applicants")
    public List<JobApplicant> getAllApplicants() {
        return jobsService.getAllApplicants();
    }
}
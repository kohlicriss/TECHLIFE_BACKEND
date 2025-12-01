package com.example.OfferLetter.Controller;

import com.example.OfferLetter.Repository.JobDetailsRepository;
import com.example.OfferLetter.Repository.OfferRepository;
import com.example.OfferLetter.Repository.PayrollRepository;
import com.example.OfferLetter.Service.JobDetailsService;
import com.example.OfferLetter.Service.OfferLetterService;
import com.example.OfferLetter.Service.OfferService;
import com.example.OfferLetter.Dto.OfferDTO;
import com.example.OfferLetter.Entity.JobDetails;
import com.example.OfferLetter.Entity.OfferLetter;
import com.example.OfferLetter.Entity.Payroll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payroll/offerletter")
public class OfferLetterController {

    @Autowired
    private OfferLetterService offerLetterService;

    @Autowired
    private OfferService offerService;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private JobDetailsRepository jobDetailsRepository;


    @GetMapping("/html/employee/{employeeId}")
    public ResponseEntity<String> getOfferLetterByEmployeeId(@PathVariable String employeeId) {
        try {
            OfferDTO offerDTO = offerService.getOfferByEmployeeId(employeeId);
            String htmlContent = offerLetterService.generateOfferLetterHtml(offerDTO);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlContent);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<html><body>Error generating offer letter: " + e.getMessage() + "</body></html>");
        }
    }

    @GetMapping("/debug/{employeeId}")
    public ResponseEntity<Map<String, Object>> debugEmployeeData(@PathVariable String employeeId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check if employee exists in job details
            List<JobDetails> jobDetails = jobDetailsRepository.findByEmployeeEmployeeIdOrderByStartDateDesc(employeeId);
            boolean jobDetailsFound = !jobDetails.isEmpty();
            response.put("jobDetailsFound", jobDetailsFound);
            response.put("jobDetailsCount", jobDetails.size());

            // Check if payroll data exists
            List<Payroll> payrollData = payrollRepository.findByEmployeeEmployeeId(employeeId);
            boolean payrollFound = !payrollData.isEmpty();
            response.put("payrollFound", payrollFound);
            response.put("payrollCount", payrollData.size());

            // Test the OfferDTO generation
            if (jobDetailsFound) {
                OfferDTO offerDTO = offerService.getOfferByEmployeeId(employeeId);
                response.put("offerDTO", offerDTO);
                response.put("status", "SUCCESS");
            } else {
                response.put("status", "EMPLOYEE_NOT_FOUND");
                response.put("message", "No job details found for employee ID: " + employeeId);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @PostMapping("/html-json")
    public ResponseEntity<Map<String, String>> getOfferLetterHtmlJson(@RequestBody OfferDTO offerDTO) {
        try {
            String htmlContent = offerLetterService.generateOfferLetterHtml(offerDTO);

            Map<String, String> response = new HashMap<>();
            response.put("htmlContent", htmlContent);
            response.put("fileName", "offer-letter-" + offerDTO.getEmpName() + ".pdf");
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate offer letter");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }


    // Test endpoint
    @GetMapping("/test-html")
    public ResponseEntity<Map<String, String>> testHtml() {
        try {
            OfferDTO offerDTO = new OfferDTO();
            offerDTO.setEmployeeId("EMP123");
            offerDTO.setEmpName("John Doe");
            offerDTO.setJobType("Full-time");
            offerDTO.setDepartment("Engineering");
            offerDTO.setDesignation("Senior Software Engineer");
            offerDTO.setAnnualSalary(850000.00);
            offerDTO.setStartDate(LocalDate.of(2024, 1, 15));

            String htmlContent = offerLetterService.generateOfferLetterHtml(offerDTO);

            Map<String, String> response = new HashMap<>();
            response.put("htmlContent", htmlContent);
            response.put("fileName", "test-offer-letter.pdf");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
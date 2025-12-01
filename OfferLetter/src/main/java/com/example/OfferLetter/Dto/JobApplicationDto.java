package com.example.OfferLetter.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobApplicationDto {
    private String studentName;
    private String stdEmail;
    private Long phoneNumber;
    private String jobTitle;
    private MultipartFile resumeFile;
}
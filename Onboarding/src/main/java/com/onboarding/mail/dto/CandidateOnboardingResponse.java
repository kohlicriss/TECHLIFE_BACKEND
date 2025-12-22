package com.onboarding.mail.dto;

import lombok.Data;

@Data
public class CandidateOnboardingResponse {

    private Long candidateId;
    private String name;
    private String email;
    private String role;

    private String phone;
    private String dob;
    private Boolean graduated;
    private String collegeName;
    private Integer passingYear;
    private String address;

    private String resumeUrl;
    private String aadhaarUrl;
    private String panUrl;
    private String photoUrl;
    private String sscMemoUrl;
    private String interMemoUrl;
    private String degreeDocUrl;
    private String introVideo;
}

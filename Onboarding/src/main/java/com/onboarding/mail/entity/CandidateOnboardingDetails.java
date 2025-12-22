package com.onboarding.mail.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data

public class CandidateOnboardingDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private LocalDate dob;
    private String phone;
    private Boolean graduated;
    private String collegeName;
    private Integer passingYear;
    private String address;

    private String resumePath;
    private String aadhaarPath;
    private String panPath;
    private String photoPath;
    private String sscMemo;
    private String interMemo;
    private String degreeDoc;


    @OneToOne
    @JoinColumn(name="candidate_id")
    private Candidate candidate;

    private String introVideo;
}

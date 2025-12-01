package com.example.OfferLetter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "applicants", schema = "payroll")
public class JobApplicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicantId;

    private String studentName;
    private String jobTitle;
    private String stdEmail;
    private Long phoneNumber;


    private String resumeFileName;
    private String resumeFileType;
    private Long resumeFileSize;


    @Column(name = "resume_file_data", columnDefinition = "OID")
    private byte[] resumeFileData;
}
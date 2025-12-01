package com.example.OfferLetter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "jobs", schema = "payroll")
public class Jobs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long jobId;

    private String jobTitle;
    private Integer experience;
    private String location;
    private String department;
    private String jobType;
    private String status;
    private String description;
    private LocalDate createdDate;
    private String skills;







    @PrePersist
    public void onCreate() {
        this.createdDate = LocalDate.now();
    }
}
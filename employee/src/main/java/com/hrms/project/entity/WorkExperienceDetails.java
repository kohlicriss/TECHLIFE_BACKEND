package com.hrms.project.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkExperienceDetails {

    @Id
    private String id;
    private String companyName;
    private String jobTitle;
    private String location;
    private String description;
    private String startMonth;
    private String startYear;
    private String endMonth;
    private String endYear;
//    private String uploadFiles;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @JsonBackReference("work")
    private Employee employee;


}
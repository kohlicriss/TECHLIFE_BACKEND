package com.hrms.project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;



@Entity
@Table(
        name = "degree_certificates",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"employee_id", "degree_type"})
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = "employee")
@ToString(exclude = "employee")
public class DegreeCertificates {

    @Id
    private String id;

    private String branchOrSpecialization;
    private String startMonth;
    private String endMonth;
    private String startYear;
    private String endYear;
    private String cgpaOrPercentage;
    private String universityOrCollege;
    private String addFiles;
    private String degreeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id",nullable = false)
    @JsonBackReference("degree")
    private Employee employee;


}
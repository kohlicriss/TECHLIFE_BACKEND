package com.hrms.project.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Archive {
    @Id
    private String employeeId;
    private String displayName;
    private String workEmail;
    private String workNumber;
    private String Gender;
    private String employeeImage;
    private LocalDate dateOfJoining;
    private LocalDate dateOfLeaving;

    private String departmentId;
    private List<String> projectId;
    private List<String> TeamId;

    private String aadharNumber;
    private String aadharImage;

    private String panNumber;
    private String panImage;

    private String passportNumber;
    private String passportImage;

    private List<String> degreeDocuments;
}

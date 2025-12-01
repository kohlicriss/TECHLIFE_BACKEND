package com.hrms.project.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobDetailsDTO {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotNull(message = "Date of joining is required")
    @FutureOrPresent(message = "Date of joining must be today or a future date")
    private LocalDate dateOfJoining;

    @NotBlank(message = "Primary job title is required")
    private String jobTitlePrimary;

    private String jobTitleSecondary;

    @NotBlank(message = "Probation status is required")
    @Pattern(regexp = "^(Yes|No)$", message = "In probation must be Yes or No")
    private String inProbation;

    private LocalDate probationStartDate;
    private LocalDate probationEndDate;

    private String probationPolicy;

    @NotBlank(message = "Worker type is required")
    private String workerType;

    @NotBlank(message = "Time type is required")
    @Pattern(regexp = "^(Full-Time|Part-Time)$", message = "Time type must be Full-Time or Part-Time")
    private String timeType;

    @NotBlank(message = "Contract status is required")
    @Pattern(regexp = "^(Permanent|Contract|Intern)$", message = "Contract status must be Permanent, Contract, or Intern")
    private String contractStatus;

    private LocalDate contractStartDate;

    private LocalDateTime shiftStartTime;
    private LocalDateTime shiftEndTime;
}

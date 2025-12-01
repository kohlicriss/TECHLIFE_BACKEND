package com.hrms.project.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DrivingLicenseDTO {

    @NotBlank(message = "License number is required")
    @Size(max = 20, message = "License number can be up to 20 characters")
    private String licenseNumber;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name can be up to 100 characters")
    private String name;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Blood group is required")
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Blood group must be one of A+, A-, B+, B-, AB+, AB-, O+, O-")
    private String bloodGroup;

    @NotBlank(message = "Father's name is required")
    @Size(max = 100, message = "Father's name can be up to 100 characters")
    private String fatherName;

    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiresOn;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address can be up to 255 characters")
    private String address;

    private String licenseImage;
}

package com.hrms.project.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AadhaarDTO {

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^[2-9]{1}[0-9]{11}$", message = "Invalid Aadhaar number format should consists of 12 digits")
    private String aadhaarNumber;

    @NotBlank(message = "Enrollment number is required")
    @Pattern(regexp = "^[0-9]{4}/[0-9]{5}/[0-9]{7}$", message = "Invalid enrollment number format (e.g., 1234/12345/1234567)")
    private String enrollmentNumber;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Aadhaar name is required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String aadhaarName;

    @NotBlank(message = "Address is required")
    @Size(min = 10, max = 200, message = "Address must be between 10 and 200 characters")
    private String address;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "Male|Female|Other", message = "Gender must be Male, Female, or Other")
    private String gender;

    private String uploadAadhaar;
}

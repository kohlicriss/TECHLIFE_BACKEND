package com.hrms.project.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PassportDetailsDTO {

    @NotBlank(message = "Passport number is required")
    @Pattern(regexp = "^[A-Z0-9]{6,9}$", message = "Invalid passport number format")
    private String passportNumber;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 3, message = "Country code must be 2 or 3 characters")
    private String countryCode;

    @NotBlank(message = "Passport type is required")
    private String passportType;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(Male|Female|Other)$", message = "Gender must be Male, Female, or Other")
    private String gender;

    @NotNull(message = "Date of issue is required")
    @PastOrPresent(message = "Date of issue must be in the past or present")
    private LocalDate dateOfIssue;

    @NotBlank(message = "Place of issue is required")
    private String placeOfIssue;

    @NotBlank(message = "Place of birth is required")
    private String placeOfBirth;

    @NotNull(message = "Date of expiration is required")
    @Future(message = "Date of expiration must be in the future")
    private LocalDate dateOfExpiration;

    @NotBlank(message = "Address is required")
    private String address;

    private String passportImage;
}

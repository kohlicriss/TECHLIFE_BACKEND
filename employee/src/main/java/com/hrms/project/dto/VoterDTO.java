package com.hrms.project.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoterDTO {

    @NotBlank(message = "Voter ID number is required")
    @Pattern(regexp = "^[A-Z]{3}[0-9]{7}$", message = "Invalid Voter ID format (e.g., ABC1234567)")
    private String voterIdNumber;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;

    @NotBlank(message = "Relation name is required")
    @Size(min = 3, max = 100, message = "Relation name must be between 3 and 100 characters")
    private String relationName;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(Male|Female|Other)$", message = "Gender must be Male, Female, or Other")
    private String gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Address is required")
    private String address;

    private String uploadVoter;

    @NotNull(message = "Issued date is required")
    @PastOrPresent(message = "Issued date must be in the past or present")
    private LocalDate issuedDate;
}

package com.hrms.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PanDTO {

    @NotBlank(message = "PAN number is required")
    @Pattern(
            regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}",
            message = "Invalid PAN format (e.g., ABCDE1234F)"
    )
    private String panNumber;

    @NotBlank(message = "PAN name is required")
    @Size(min = 3, max = 100, message = "PAN name must be between 3 and 100 characters")
    private String panName;

    @NotBlank(message = "Date of birth is required")
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}$",
            message = "Date of birth must be in YYYY-MM-DD format"
    )
    private String dateOfBirth;

    @NotBlank(message = "Parent's name is required")
    private String parentsName;

    private String panImage;
}

package com.hrms.project.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkExperienceDTO {

//    @NotBlank(message = "ID is required")
//    private String id;

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;

    @NotBlank(message = "Job title is required")
    @Size(min = 2, max = 100, message = "Job title must be between 2 and 100 characters")
    private String jobTitle;

    @NotBlank(message = "Location is required")
    private String location;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotBlank(message = "Start month is required")
    @Pattern(regexp = "(?i)^(January|February|March|April|May|June|July|August|September|October|November|December)$",
            message = "Start month must be a valid month name")
    private String startMonth;

    @NotBlank(message = "End month is required")
    @Pattern(regexp = "(?i)^(January|February|March|April|May|June|July|August|September|October|November|December)$",
            message = "End month must be a valid month name")
    private String endMonth;

    @NotBlank(message = "Start year is required")
    @Pattern(regexp = "^(19|20)\\d{2}$", message = "Start year must be a valid 4-digit year")
    private String startYear;



    @NotBlank(message = "End year is required")
    @Pattern(regexp = "^(19|20)\\d{2}$", message = "End year must be a valid 4-digit year")
    private String endYear;
}

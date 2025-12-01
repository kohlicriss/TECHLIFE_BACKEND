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
public class DegreeDTO {

    private String id;

    @NotBlank(message = "Branch or Specialization is required")
    @Size(max = 100, message = "Branch or Specialization can be up to 100 characters")
    private String branchOrSpecialization;

    @NotBlank(message = "Start month is required")
    @Pattern(regexp = "^(January|February|March|April|May|June|July|August|September|October|November|December)$",
            message = "Start month must be a valid month name")
    private String startMonth;

    @NotBlank(message = "End month is required")
    @Pattern(regexp = "^(January|February|March|April|May|June|July|August|September|October|November|December)$",
            message = "End month must be a valid month name")
    private String endMonth;

    @NotBlank(message = "Start year is required")
    @Pattern(regexp = "^(19|20)\\d{2}$", message = "Start year must be a valid year between 1900–2099")
    private String startYear;

    @NotBlank(message = "End year is required")
    @Pattern(regexp = "^(19|20)\\d{2}$", message = "End year must be a valid year between 1900–2099")
    private String endYear;

    @NotBlank(message = "CGPA or Percentage is required")
    @Pattern(regexp = "^\\d{1,2}(\\.\\d{1,2})?$|^100$", message = "CGPA or Percentage must be a number between 0 and 100")
    private String cgpaOrPercentage;

    @NotBlank(message = "University or College name is required")
    @Size(max = 200, message = "University or College name can be up to 200 characters")
    private String universityOrCollege;


    @NotBlank(message = "Degree name is required")
    @Size(max = 100, message = "Degree name can be up to 100 characters")
    private String degreeType;

    private String addFiles;
}

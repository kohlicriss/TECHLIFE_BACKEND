package com.hrms.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AchievementsDTO {

    private String id;


    @NotBlank(message = "Certification name is required")
    @Size(min = 2, max = 100, message = "Certification name must be between 2 and 100 characters")
    private String certificationName;

    @NotBlank(message = "Issuing authority is required")
    @Size(min = 2, max = 100, message = "Issuing authority name must be between 2 and 100 characters")
    private String issuingAuthorityName;

    @Size(max = 200, message = "Certification URL must be less than 200 characters")
    @Pattern(regexp = "^(http[s]?://.*)?$", message = "Invalid URL format")
    private String certificationURL;

    @Pattern(regexp = "^(January|February|March|April|May|June|July|August|September|October|November|December)?$",
            message = "Invalid issue month")
    private String issueMonth;

    @Pattern(regexp = "^(19|20)\\d{2}?$", message = "Invalid issue year")
    private String issueYear;

    @Pattern(regexp = "^(January|February|March|April|May|June|July|August|September|October|November|December)?$",
            message = "Invalid expiration month")
    private String expirationMonth;

    @Pattern(regexp = "^(19|20)\\d{2}?$", message = "Invalid expiration year")
    private String expirationYear;

    @Size(max = 50, message = "License number must be less than 50 characters")
    private String licenseNumber;

    private String achievementFile;
}

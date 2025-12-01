package com.hrms.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactDetailsDTO {



    @Email(message = "Work email should be valid")
    @NotBlank(message = "Work email is required")
    private String workEmail;

    @Email(message = "Personal email should be valid")
    private String personalEmail;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit Indian number")
    private String mobileNumber;

    @Pattern(regexp = "^$|^[0-9]{3,15}$", message = "Work number must be between 3 to 15 digits")
    private String workNumber;
}

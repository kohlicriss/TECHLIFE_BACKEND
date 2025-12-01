package com.hrms.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePrimaryDetailsDTO {

 @NotBlank(message = "First name is required")
 @Size(max = 50, message = "First name can be up to 50 characters")
 private String firstName;

 @Size(max = 50, message = "Middle name can be up to 50 characters")
 private String middleName;

 @NotBlank(message = "Last name is required")
 @Size(max = 50, message = "Last name can be up to 50 characters")
 private String lastName;

 @NotBlank(message = "Display name is required")
 @Size(max = 100, message = "Display name can be up to 100 characters")
 private String displayName;

 @NotBlank(message = "Marital status is required")
 @Pattern(regexp = "^(Single|Married|Divorced|Widowed)$", message = "Marital status must be Single, Married, Divorced, or Widowed")
 private String maritalStatus;

 @NotBlank(message = "Blood group is required")
 @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Blood group must be one of A+, A-, B+, B-, AB+, AB-, O+, O-")
 private String bloodGroup;

 @NotBlank(message = "Physically handicapped field is required")
 @Pattern(regexp = "^(Yes|No)$", message = "Physically handicapped must be Yes or No")
 private String physicallyHandicapped;

 @NotBlank(message = "Nationality is required")
 @Size(max = 50, message = "Nationality can be up to 50 characters")
 private String nationality;

 @NotBlank(message = "Gender is required")
 @Pattern(regexp = "^(Male|Female|Other)$", message = "Gender must be Male, Female, or Other")
 private String gender;

 @NotNull(message = "Date of birth is required")
 @Past(message = "Date of birth must be in the past")
 private LocalDate dateOfBirth;
}

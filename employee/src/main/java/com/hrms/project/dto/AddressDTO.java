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
public class AddressDTO {

    @NotBlank(message = "Street is required")
    @Size(max = 100, message = "Street name must be less than 100 characters")
    private String street;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City name must be less than 50 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State name must be less than 50 characters")
    private String state;

    @NotBlank(message = "ZIP code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "ZIP code must be a valid 6-digit number (India)")
    private String zip;

    @NotBlank(message = "Country is required")
    @Size(max = 50, message = "Country name must be less than 50 characters")
    private String country;

    @NotBlank(message = "District is required")
    @Size(max = 50, message = "District name must be less than 50 characters")
    private String district;
}

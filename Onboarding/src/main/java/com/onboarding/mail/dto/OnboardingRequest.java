package com.onboarding.mail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OnboardingRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String dob;

    @NotBlank
    private String phone;

    @NotNull
    private Boolean graduated;

    private String collegeName;

    private Integer passingYear;

    @NotBlank
    private String address;
}

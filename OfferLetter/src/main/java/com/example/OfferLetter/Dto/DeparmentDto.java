package com.example.OfferLetter.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeparmentDto {
    private String employeeId;
    private String empName;
    private String email;
    private String phoneNumber;
    private String designation;
    private String department;
    private String jobType;
    private String level;
    private Double annualSalary;
    private Double monthlySalary;

    private Double stipend;

    private Long accountNumber;
    private String ifsccode;
    private String bankName;

    private String pfnum;
    private String panNumber;
    private Long aadharNumber;
    private String uanNumber;
    private LocalDate startDate;
}
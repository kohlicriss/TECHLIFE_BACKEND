package com.example.OfferLetter.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
class EmployeeDTO {
    private String employeeId;
    private String empName;
    private String email;
    private String phoneNumber;
    private Double annualSalary;


    private Double stipend;
    private Double monthlySalary;
    private Long accountNumber;
    private String bankName;
    private String panNumber;
    private Long aadharNumber;
    private String ifsccode;
    private String pfnum;
    private String uanNumber;
    private Double pfDeductionRate;
    private Double professionalTax;



    private String jobType;
    private String level;
    private LocalDate startDate;
}
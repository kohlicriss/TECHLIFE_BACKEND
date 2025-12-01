package com.example.OfferLetter.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class OfferDTO {
    private String employeeId;
    private String empName;
    private String jobType;
    private String department;
    private String designation;
    private Double annualSalary;
    private Double totalSalaryPerMonth;
    private Double monthlySalary;

    private Double stipend;
    private LocalDate startDate;

    private Double attendanceDeduction;

    private Double basicSalary;
    private Double hraAmount;
    private Double conveyanceAllowance;
    private Double medicalAllowance;
    private Double specialAllowance;
    private Double bonusAmount;
    private Double otherAllowances;
    private Double providentFund;
    private Double professionalTax;
    private Double incomeTax;
    private Double otherDeductions;
    private Double grossEarnings;
    private Double totalDeductions;
    private Double netSalary;
}
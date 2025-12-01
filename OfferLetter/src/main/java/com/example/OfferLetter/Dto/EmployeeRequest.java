package com.example.OfferLetter.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeRequest {
    private String employeeId;
    private String empName;
    private String email;
    private String phoneNumber;
    private Double annualSalary;
    private Double monthlySalary;
    private Long accountNumber;
    private String ifsccode;
    private String bankName;
    private String pfnum;
    private String panNumber;
    private Long aadharNumber;
    private String uanNumber;
    private String department;
    private String designation;

    private Double basicSalary;
    private Double hraAmount;
    private Double conveyanceAllowance;
    private Double medicalAllowance;
    private Double specialAllowance;
    private Double bonusAmount;
    private Double otherAllowances;
    private Double providentFund;

    private Double attendanceDeduction;

    private Double incomeTax;
    private Double otherDeductions;
    private Double grossEarnings;
    private Double totalDeductions;
    private Double netSalary;

    private Double stipend;

    private String JobType;
    private String Level;

    private LocalDate startDate;
}
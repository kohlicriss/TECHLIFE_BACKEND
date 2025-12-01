package com.example.OfferLetter.Dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayrollDTO {
    private Long payrollId;
    private Integer month;
    private Integer year;
    private LocalDate payDate;
    private Integer totalWorkingDays;
    private Float daysPresent;
    private Float paidDays;
    private Float lossOfPayDays;
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
    private Double attendanceDeduction;

    private Double grossEarnings;
    private Double totalDeductions;
    private Double netSalary;
    private Double totalSalaryPerMonth;
    private Double monthlySalary;
    private String status;
    private EmployeeDTO employee;

    private Double stipend;
}
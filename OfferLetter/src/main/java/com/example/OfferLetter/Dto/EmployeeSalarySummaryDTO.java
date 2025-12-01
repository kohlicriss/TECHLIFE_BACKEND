package com.example.OfferLetter.Dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeSalarySummaryDTO {
    private Long payrollId;
    private String employeeId;
    private String employeeName;
    private Integer month;
    private Integer year;
    private Double bonusAmount;
    private Double totalSalary;
    private Double totalDeductions;
    private Double netSalary;

    private String pfnum;
    private String panNumber;
    private String ifsccode;
    private String designation;

    private Double stipend;

    private Double attendanceDeduction;

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
    private Double otherAllowances;
    private Double providentFund;
    private Double professionalTax;
    private Double incomeTax;
    private Double otherDeductions;
    private Double grossEarnings;

    private String status;


}
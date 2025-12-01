package com.example.OfferLetter.Dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePayrollRequest {
    private Double bonusAmount;
    private Double hikePercentage;
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

    private Double attendanceDeduction;

    private Double stipend;


}
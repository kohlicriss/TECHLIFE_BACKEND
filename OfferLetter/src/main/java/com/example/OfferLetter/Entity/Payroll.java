package com.example.OfferLetter.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.DecimalFormat;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "payrolls", schema = "payroll")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long payrollId;

    private Integer month;
    private Integer year;
    private LocalDate payDate;
    private Integer totalWorkingDays;
    private Float daysPresent;
    private Float paidDays;
    private Float lossOfPayDays;



    private Double stipend;

    private Double basicSalary;
    private Double hraAmount;
    private Double conveyanceAllowance;
    private Double medicalAllowance;
    private Double specialAllowance;


    private Double bonusAmount = 0.0;
    private Double hikePercentage = 0.0;
    private Double hikeAmount = 0.0;
    private Double otherAllowances = 0.0;


    private Double providentFund;
    private Double professionalTax;
    private Double incomeTax;
    private Double otherDeductions = 0.0;


    private Double grossEarnings;
    private Double totalDeductions;
    private Double netSalary;

    private Double monthlySalary;

    private Double dailySalary;
    private Double attendanceDeduction;


    private Double totalSalaryPerMonth;

    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @JsonIgnoreProperties({"payrolls", "jobDetailsList", "attendanceRecords"})
    private Employee employee;

    //@PrePersist
    //@PreUpdate
    public void calculateTotals() {

        this.grossEarnings = (this.basicSalary != null ? this.basicSalary : 0) +
                (this.hraAmount != null ? this.hraAmount : 0) +
                (this.conveyanceAllowance != null ? this.conveyanceAllowance : 0) +
                (this.medicalAllowance != null ? this.medicalAllowance : 0) +
                (this.specialAllowance != null ? this.specialAllowance : 0) +
                (this.bonusAmount != null ? this.bonusAmount : 0) +
                (this.otherAllowances != null ? this.otherAllowances : 0);

        // Calculate total deductions INCLUDING attendance deduction
        double statutoryDeductions = (this.providentFund != null ? this.providentFund : 0) +
                (this.professionalTax != null ? this.professionalTax : 0) +
                (this.incomeTax != null ? this.incomeTax : 0) +
                (this.otherDeductions != null ? this.otherDeductions : 0);

        double attendanceDed = this.attendanceDeduction != null ? this.attendanceDeduction : 0;

        this.totalDeductions = statutoryDeductions + attendanceDed;

        // Calculate net salary correctly: monthlySalary - totalDeductions
        if (this.monthlySalary != null) {
            this.netSalary = this.monthlySalary - this.totalDeductions;
        } else {
            // Fallback to old calculation if monthlySalary is not set
            this.netSalary = this.grossEarnings - this.totalDeductions;
        }


        formatDecimalValues();
    }





    private void formatDecimalValues() {
        DecimalFormat df = new DecimalFormat("#.##");

        this.basicSalary = Double.valueOf(df.format(this.basicSalary != null ? this.basicSalary : 0));
        this.hraAmount = Double.valueOf(df.format(this.hraAmount != null ? this.hraAmount : 0));
        this.conveyanceAllowance = Double.valueOf(df.format(this.conveyanceAllowance != null ? this.conveyanceAllowance : 0));
        this.medicalAllowance = Double.valueOf(df.format(this.medicalAllowance != null ? this.medicalAllowance : 0));
        this.specialAllowance = Double.valueOf(df.format(this.specialAllowance != null ? this.specialAllowance : 0));
        this.bonusAmount = Double.valueOf(df.format(this.bonusAmount != null ? this.bonusAmount : 0));
        this.hikeAmount = Double.valueOf(df.format(this.hikeAmount != null ? this.hikeAmount : 0));
        this.otherAllowances = Double.valueOf(df.format(this.otherAllowances != null ? this.otherAllowances : 0));
        this.providentFund = Double.valueOf(df.format(this.providentFund != null ? this.providentFund : 0));
        this.professionalTax = Double.valueOf(df.format(this.professionalTax != null ? this.professionalTax : 0));
        this.incomeTax = Double.valueOf(df.format(this.incomeTax != null ? this.incomeTax : 0));
        this.otherDeductions = Double.valueOf(df.format(this.otherDeductions != null ? this.otherDeductions : 0));
        this.grossEarnings = Double.valueOf(df.format(this.grossEarnings != null ? this.grossEarnings : 0));
        this.totalDeductions = Double.valueOf(df.format(this.totalDeductions != null ? this.totalDeductions : 0));
        this.netSalary = Double.valueOf(df.format(this.netSalary != null ? this.netSalary : 0));
        this.totalSalaryPerMonth = Double.valueOf(df.format(this.totalSalaryPerMonth != null ? this.totalSalaryPerMonth : 0));
    }

}
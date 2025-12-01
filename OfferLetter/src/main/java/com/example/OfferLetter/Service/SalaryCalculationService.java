package com.example.OfferLetter.Service;

import com.example.OfferLetter.Dto.UpdatePayrollRequest;
import com.example.OfferLetter.Entity.Attendance;
import com.example.OfferLetter.Entity.Employee;
import com.example.OfferLetter.Entity.Payroll;
import com.example.OfferLetter.Repository.AttendanceRepository;
import com.example.OfferLetter.Repository.EmployeeRepository;
import com.example.OfferLetter.Repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class SalaryCalculationService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private NotificationService notificationService;

    private static final double BASIC_PERCENTAGE = 0.50;
    private static final double HRA_PERCENTAGE = 0.40;
    private static final double CONVEYANCE_ALLOWANCE = 1600.0;
    private static final double MEDICAL_ALLOWANCE = 1250.0;

    private static final double PROFESSIONAL_TAX = 200.0;

    public Payroll calculateSalary(String employeeId, Integer month, Integer year) {
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));

        Attendance attendance = attendanceRepository.findByEmployeeAndMonthYear(employeeId, month, year)
                .orElseThrow(() -> new RuntimeException("Attendance record not found for employee: " + employeeId + " for " + month + "/" + year));


        Double monthlySalary = calculateMonthlySalaryFromAnnual(employee.getAnnualSalary());


        Map<String, Double> salaryComponents = calculateSalaryComponents(monthlySalary);


        Map<String, Double> deductions = calculateDeductions(salaryComponents, employee);


        Map<String, Double> attendanceAdjustments = applyAttendanceDeductions(monthlySalary, attendance);


        double totalStatutoryDeductions = calculateTotalDeductions(deductions);
        double attendanceDeduction = attendanceAdjustments.get("attendanceDeduction");


        double totalDeductions = totalStatutoryDeductions + attendanceDeduction;


        double finalNetSalary = monthlySalary - totalDeductions ;


        Payroll payroll = payrollRepository.findByEmployeeEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElse(new Payroll());

        boolean isNewPayroll = payroll.getPayrollId() == null;

        populatePayrollRecord(payroll, employee, month, year, attendance,
                salaryComponents, deductions, attendanceAdjustments,  monthlySalary);

        Payroll savedPayroll = payrollRepository.save(payroll);

        // Debug: Verify what was saved
        System.out.println("=== AFTE666R SAVE VERIFICATION ===");
        System.out.println("Saved Total Deductions: " + savedPayroll.getTotalDeductions());
        System.out.println("Saved Net Salary: " + savedPayroll.getNetSalary());

        if (isNewPayroll) {
            notificationService.sendSalaryUploadNotification(savedPayroll, employee);
        }

        return savedPayroll;
    }


    private Double calculateMonthlySalaryFromAnnual(Double annualSalary) {
        if (annualSalary == null || annualSalary == 0) {
            throw new RuntimeException("Annual salary is not set for employee");
        }
        // Convert LPA to rupees (11 LPA = 11,00,000 ₹)
        return (annualSalary * 100000) / 12.0;
    }


    private Map<String, Double> calculateSalaryComponents(Double monthlySalary) {
        Map<String, Double> components = new HashMap<>();

        double basicSalary = monthlySalary * BASIC_PERCENTAGE;
        double hraAmount = basicSalary * HRA_PERCENTAGE;
        double specialAllowance = monthlySalary - (basicSalary + hraAmount + CONVEYANCE_ALLOWANCE + MEDICAL_ALLOWANCE);


        if (specialAllowance < 0) {
            specialAllowance = 0;

            double adjustedAmount = monthlySalary - (hraAmount + CONVEYANCE_ALLOWANCE + MEDICAL_ALLOWANCE);
            if (adjustedAmount > 0) {
                basicSalary = adjustedAmount;
            }
        }

        components.put("basicSalary", basicSalary);
        components.put("hraAmount", hraAmount);
        components.put("conveyanceAllowance", CONVEYANCE_ALLOWANCE);
        components.put("medicalAllowance", MEDICAL_ALLOWANCE);
        components.put("specialAllowance", specialAllowance);
        components.put("bonusAmount", 0.0);
        components.put("otherAllowances", 0.0);

        return components;
    }


    private Map<String, Double> calculateDeductions(Map<String, Double> salaryComponents, Employee employee) {
        Map<String, Double> deductions = new HashMap<>();

        double basicSalary = salaryComponents.get("basicSalary");


        double providentFund = basicSalary * 0.12;


        double professionalTax = PROFESSIONAL_TAX;


        double incomeTax = calculateIncomeTax(employee.getAnnualSalary()) / 12;

        deductions.put("providentFund", providentFund);
        deductions.put("professionalTax", professionalTax);
        deductions.put("incomeTax", incomeTax);
        deductions.put("otherDeductions", 0.0);

        return deductions;
    }

    private Map<String, Double> applyAttendanceDeductions(double monthlySalary, Attendance attendance) {
        Map<String, Double> adjustments = new HashMap<>();

        float totalWorkingDays = attendance.getTotalWorkingDays();
        float daysPresent = attendance.getDaysPresent();
        float absentDays = totalWorkingDays - daysPresent;

        // Calculate salary per day
        double salaryPerDay = monthlySalary / totalWorkingDays;

        // Calculate deduction for absent days
        double attendanceDeduction = absentDays * salaryPerDay;

        adjustments.put("salaryPerDay", salaryPerDay);
        adjustments.put("absentDays", (double) absentDays);
        adjustments.put("attendanceDeduction", attendanceDeduction);
        adjustments.put("dailySalary", salaryPerDay);
        adjustments.put("paidDays", (double) daysPresent);

        return adjustments;
    }

    // Helper method to calculate total deductions
    private double calculateTotalDeductions(Map<String, Double> deductions) {
        return deductions.get("providentFund")
                + deductions.get("professionalTax")
                + deductions.get("incomeTax")
                + deductions.get("otherDeductions");
    }


    private double calculateInitialNetSalary(Map<String, Double> salaryComponents, Map<String, Double> deductions) {
        double grossEarnings = salaryComponents.get("basicSalary")
                + salaryComponents.get("hraAmount")
                + salaryComponents.get("conveyanceAllowance")
                + salaryComponents.get("medicalAllowance")
                + salaryComponents.get("specialAllowance")
                + salaryComponents.get("bonusAmount")
                + salaryComponents.get("otherAllowances");

        double totalDeductions = deductions.get("providentFund")
                + deductions.get("professionalTax")
                + deductions.get("incomeTax")
                + deductions.get("otherDeductions");

        return grossEarnings - totalDeductions;
    }





    private double calculateIncomeTax(Double annualSalary) {
        if (annualSalary == null) return 0.0;

        // Convert LPA to rupees for tax calculation (11 LPA = 11,00,000 ₹)
        double annualSalaryInRupees = annualSalary * 100000;
        double taxableIncome = annualSalaryInRupees - 50000;

        double tax = 0.0;

        if (taxableIncome <= 250000) {
            tax = 0;
        } else if (taxableIncome <= 500000) {
            tax = (taxableIncome - 250000) * 0.05;
        } else if (taxableIncome <= 1000000) {
            tax = 12500 + (taxableIncome - 500000) * 0.20;
        } else {
            tax = 112500 + (taxableIncome - 1000000) * 0.30;
        }

        // Add health and education cess (4%)
        tax = tax * 1.04;

        return Math.max(tax, 0);
    }

    private void populatePayrollRecord(Payroll payroll, Employee employee, Integer month, Integer year,
                                       Attendance attendance, Map<String, Double> salaryComponents,
                                       Map<String, Double> deductions, Map<String, Double> attendanceAdjustments,
                                        double monthlySalary) {

        payroll.setEmployee(employee);
        payroll.setMonth(month);
        payroll.setYear(year);
        payroll.setPayDate(LocalDate.now());

        // Attendance information
        payroll.setTotalWorkingDays(attendance.getTotalWorkingDays());
        payroll.setDaysPresent(attendance.getDaysPresent());
        payroll.setPaidDays(attendance.getDaysPresent());
        payroll.setLossOfPayDays(attendanceAdjustments.get("absentDays").floatValue());

        // Salary components
        payroll.setBasicSalary(salaryComponents.get("basicSalary"));
        payroll.setHraAmount(salaryComponents.get("hraAmount"));
        payroll.setConveyanceAllowance(salaryComponents.get("conveyanceAllowance"));
        payroll.setMedicalAllowance(salaryComponents.get("medicalAllowance"));
        payroll.setSpecialAllowance(salaryComponents.get("specialAllowance"));
        payroll.setBonusAmount(salaryComponents.get("bonusAmount"));
        payroll.setOtherAllowances(salaryComponents.get("otherAllowances"));

        // Deductions
        payroll.setProvidentFund(deductions.get("providentFund"));
        payroll.setProfessionalTax(deductions.get("professionalTax"));
        payroll.setIncomeTax(deductions.get("incomeTax"));
        payroll.setOtherDeductions(deductions.get("otherDeductions"));

        //double attendanceDeduction = attendanceAdjustments.get("attendanceDeduction");



        double grossEarnings = salaryComponents.get("basicSalary")
                + salaryComponents.get("hraAmount")
                + salaryComponents.get("conveyanceAllowance")
                + salaryComponents.get("medicalAllowance")
                + salaryComponents.get("specialAllowance")
                + salaryComponents.get("bonusAmount")
                + salaryComponents.get("otherAllowances");

        payroll.setGrossEarnings(grossEarnings);

        double statutoryDeductions = deductions.get("providentFund")
                + deductions.get("professionalTax")
                + deductions.get("incomeTax")
                + deductions.get("otherDeductions");

        double attendanceDeduction = attendanceAdjustments.get("attendanceDeduction");
        payroll.setAttendanceDeduction(attendanceDeduction);


        double totalDeductions = statutoryDeductions + attendanceDeduction;

        payroll.setTotalDeductions(totalDeductions);
        payroll.setAttendanceDeduction(attendanceDeduction);


        //payroll.setNetSalary(grossEarnings - totalDeductions);

        // Additional information
        payroll.setDailySalary(attendanceAdjustments.get("dailySalary"));
        payroll.setMonthlySalary(monthlySalary);

        payroll.setStatus("CALCULATED");

        payroll.setNetSalary(grossEarnings - totalDeductions);

        // Debug logging - fix the variable name
        System.out.println("=== POPULATE PAYROLL DEBUG ===");
        System.out.println("Monthly Salary: " + monthlySalary);
        System.out.println("Statutory Deductions: " + statutoryDeductions);
        System.out.println("Attendance Deduction: " + attendanceDeduction);
        System.out.println("Total Deductions Set: " + totalDeductions);
        System.out.println("Gross Earnings: " + grossEarnings);
        System.out.println("Net Salary Set: " + (grossEarnings - totalDeductions)); // Fixed
        System.out.println("=== END DEBUG ===");
    }


    public Payroll updatePayrollComponents(Long payrollId, UpdatePayrollRequest request) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll record not found"));

        Employee employee = payroll.getEmployee();

        // Store current values for calculation and notification
        Double currentGrossEarnings = payroll.getGrossEarnings();
        Double currentNetSalary = payroll.getNetSalary();
        Double currentBonus = payroll.getBonusAmount() != null ? payroll.getBonusAmount() : 0.0;
        Double currentOtherAllowances = payroll.getOtherAllowances() != null ? payroll.getOtherAllowances() : 0.0;
        Double currentOtherDeductions = payroll.getOtherDeductions() != null ? payroll.getOtherDeductions() : 0.0;

        boolean bonusUpdated = false;
        Double oldBonusValue = currentBonus;


        if (request.getBonusAmount() != null && !request.getBonusAmount().equals(currentBonus)) {
            Double bonusDifference = request.getBonusAmount() - currentBonus;
            payroll.setBonusAmount(request.getBonusAmount());


            if (currentGrossEarnings != null) {
                payroll.setGrossEarnings(currentGrossEarnings + bonusDifference);
            }


            Double currentTotalDeductions = payroll.getTotalDeductions();
            if (currentGrossEarnings != null && currentTotalDeductions != null) {
                payroll.setNetSalary((currentGrossEarnings + bonusDifference) - currentTotalDeductions);
            }

            bonusUpdated = true;
        }

        // Update other components
        if (request.getHikePercentage() != null) {
            payroll.setHikePercentage(request.getHikePercentage());
            Double basicSalary = payroll.getBasicSalary();
            if (basicSalary != null && request.getHikePercentage() > 0) {
                Double hikeAmount = basicSalary * (request.getHikePercentage() / 100);
                payroll.setHikeAmount(hikeAmount);
                if (currentGrossEarnings != null) {
                    payroll.setGrossEarnings(currentGrossEarnings + hikeAmount);
                }
                if (currentNetSalary != null) {
                    payroll.setNetSalary(currentNetSalary + hikeAmount);
                }
            }
        }

        if (request.getOtherAllowances() != null && !request.getOtherAllowances().equals(currentOtherAllowances)) {
            Double allowanceDifference = request.getOtherAllowances() - currentOtherAllowances;
            payroll.setOtherAllowances(request.getOtherAllowances());
            if (currentGrossEarnings != null) {
                payroll.setGrossEarnings(currentGrossEarnings + allowanceDifference);
            }
            if (currentNetSalary != null) {
                payroll.setNetSalary(currentNetSalary + allowanceDifference);
            }
        }

        if (request.getOtherDeductions() != null && !request.getOtherDeductions().equals(currentOtherDeductions)) {
            Double deductionDifference = request.getOtherDeductions() - currentOtherDeductions;
            payroll.setOtherDeductions(request.getOtherDeductions());
            Double currentTotalDeductions = payroll.getTotalDeductions() != null ? payroll.getTotalDeductions() : 0.0;
            payroll.setTotalDeductions(currentTotalDeductions + deductionDifference);
            if (currentNetSalary != null) {
                payroll.setNetSalary(currentNetSalary - deductionDifference);
            }
        }

        Payroll updatedPayroll = payrollRepository.save(payroll);

        // Send notification if bonus was updated
        if (bonusUpdated && request.getBonusAmount() != null) {
            notificationService.sendBonusUpdatedNotification(updatedPayroll, employee, oldBonusValue, request.getBonusAmount());
        }

        return updatedPayroll;
    }

    public Payroll recalculateSalary(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll record not found"));

        Employee employee = payroll.getEmployee();

        Payroll recalculatedPayroll = calculateSalary(employee.getEmployeeId(), payroll.getMonth(), payroll.getYear());


        notificationService.sendSalaryRecalculatedNotification(recalculatedPayroll, employee);

        return recalculatedPayroll;
    }

    public Payroll addBonus(Long payrollId, Double bonusAmount) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll record not found"));

        Employee employee = payroll.getEmployee();

        Double currentBonus = payroll.getBonusAmount() != null ? payroll.getBonusAmount() : 0.0;
        Double newBonus = currentBonus + bonusAmount;

        payroll.setBonusAmount(newBonus);

        Double currentGrossEarnings = payroll.getGrossEarnings();
        Double currentTotalDeductions = payroll.getTotalDeductions();


        if (currentGrossEarnings != null) {
            payroll.setGrossEarnings(currentGrossEarnings + bonusAmount);
        }


        if (currentGrossEarnings != null && currentTotalDeductions != null) {
            payroll.setNetSalary((currentGrossEarnings + bonusAmount) - currentTotalDeductions);
        }

        Payroll updatedPayroll = payrollRepository.save(payroll);

        // Send notification for bonus addition
        notificationService.sendBonusAddedNotification(updatedPayroll, employee, bonusAmount);

        return updatedPayroll;
    }

    public Payroll updateBonus(Long payrollId, Double newBonusAmount) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll record not found"));

        Employee employee = payroll.getEmployee();

        Double currentBonus = payroll.getBonusAmount() != null ? payroll.getBonusAmount() : 0.0;


        if (!currentBonus.equals(newBonusAmount)) {
            Double bonusDifference = newBonusAmount - currentBonus;
            payroll.setBonusAmount(newBonusAmount);

            Double currentGrossEarnings = payroll.getGrossEarnings();
            Double currentTotalDeductions = payroll.getTotalDeductions();


            if (currentGrossEarnings != null) {
                payroll.setGrossEarnings(currentGrossEarnings + bonusDifference);
            }


            if (currentGrossEarnings != null && currentTotalDeductions != null) {
                payroll.setNetSalary((currentGrossEarnings + bonusDifference) - currentTotalDeductions);
            }

            Payroll updatedPayroll = payrollRepository.save(payroll);


            notificationService.sendBonusUpdatedNotification(updatedPayroll, employee, currentBonus, newBonusAmount);

            return updatedPayroll;
        }

        return payroll;
    }
}
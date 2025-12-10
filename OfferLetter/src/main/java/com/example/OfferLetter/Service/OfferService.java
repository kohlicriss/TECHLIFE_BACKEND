package com.example.OfferLetter.Service;

import com.example.OfferLetter.Repository.EmployeeRepository;
import com.example.OfferLetter.Repository.JobDetailsRepository;
import com.example.OfferLetter.Repository.PayrollRepository;
import com.example.OfferLetter.Dto.OfferDTO;
import com.example.OfferLetter.Entity.Employee;
import com.example.OfferLetter.Entity.JobDetails;
import com.example.OfferLetter.Entity.Payroll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OfferService {

    @Autowired
    private JobDetailsRepository jobDetailsRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public OfferDTO getOfferByEmployeeId(String employeeId) {

        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));


        List<JobDetails> jobDetailsList = jobDetailsRepository.findByEmployeeEmployeeIdOrderByStartDateDesc(employeeId);
        if (jobDetailsList.isEmpty()) {
            throw new RuntimeException("Job details not found for employee ID: " + employeeId);
        }
        JobDetails latestJobDetails = jobDetailsList.get(0);


        return convertToOfferDTO(employee, latestJobDetails);
    }

    private OfferDTO convertToOfferDTO(Employee employee, JobDetails jobDetails) {
        OfferDTO offerDTO = new OfferDTO();


        offerDTO.setEmployeeId(employee.getEmployeeId());
        offerDTO.setEmpName(employee.getEmpName());
        offerDTO.setJobType(jobDetails.getJobType());
        offerDTO.setDepartment(jobDetails.getDepartment());
        offerDTO.setDesignation(jobDetails.getDesignation());
        offerDTO.setStartDate(jobDetails.getStartDate());


        setSalaryInformationFromAnnualSalary(offerDTO, employee);

        return offerDTO;
    }

    private void setSalaryInformationFromAnnualSalary(OfferDTO offerDTO, Employee employee) {
        if (employee.getAnnualSalary() != null && employee.getAnnualSalary() > 0) {
            try {

                Double annualSalaryInRupees = employee.getAnnualSalary() * 100000;
                Double monthlySalary = annualSalaryInRupees / 12.0;


                Double basicSalary = monthlySalary * 0.50;
                Double hraAmount = basicSalary * 0.40;
                Double conveyanceAllowance = 1600.0;
                Double medicalAllowance = 1250.0;
                Double specialAllowance = monthlySalary - (basicSalary + hraAmount + conveyanceAllowance + medicalAllowance);


                if (specialAllowance < 0) {
                    specialAllowance = 0.0;
                    double adjustedAmount = monthlySalary - (hraAmount + conveyanceAllowance + medicalAllowance);
                    if (adjustedAmount > 0) {
                        basicSalary = adjustedAmount;
                    }
                }


                Double providentFund = basicSalary * 0.12;
                Double professionalTax = 200.0;
                Double incomeTax = calculateMonthlyIncomeTax(annualSalaryInRupees);


                Double grossEarnings = basicSalary + hraAmount + conveyanceAllowance + medicalAllowance + specialAllowance;
                Double totalDeductions = providentFund + professionalTax + incomeTax;
                Double netSalary = grossEarnings - totalDeductions;


                offerDTO.setMonthlySalary(monthlySalary);
                offerDTO.setBasicSalary(basicSalary);
                offerDTO.setHraAmount(hraAmount);
                offerDTO.setConveyanceAllowance(conveyanceAllowance);
                offerDTO.setMedicalAllowance(medicalAllowance);
                offerDTO.setSpecialAllowance(specialAllowance);
                offerDTO.setBonusAmount(0.0);
                offerDTO.setOtherAllowances(0.0);
                offerDTO.setProvidentFund(providentFund);
                offerDTO.setProfessionalTax(professionalTax);
                offerDTO.setIncomeTax(incomeTax);
                offerDTO.setOtherDeductions(0.0);
                offerDTO.setGrossEarnings(grossEarnings);
                offerDTO.setTotalDeductions(totalDeductions);
                offerDTO.setNetSalary(netSalary);
                offerDTO.setAnnualSalary(annualSalaryInRupees);
                offerDTO.setTotalSalaryPerMonth(grossEarnings);
                offerDTO.setStipend(employee.getStipend());

            } catch (Exception e) {

                setDefaultSalaryValues(offerDTO);
               // offerDTO.setError("Salary calculation failed: " + e.getMessage());
            }
        } else {
            setDefaultSalaryValues(offerDTO);
            //offerDTO.setError("Annual salary not set for employee");
        }
    }

    private Double calculateMonthlyIncomeTax(Double annualSalaryInRupees) {
        if (annualSalaryInRupees == null) return 0.0;

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


        tax = tax * 1.04;
        return Math.max(tax, 0) / 12;
    }

    private void setDefaultSalaryValues(OfferDTO offerDTO) {
        // Set all salary fields to 0.0 if no salary data exists
        offerDTO.setMonthlySalary(0.0);
        offerDTO.setBasicSalary(0.0);
        offerDTO.setHraAmount(0.0);
        offerDTO.setConveyanceAllowance(0.0);
        offerDTO.setMedicalAllowance(0.0);
        offerDTO.setSpecialAllowance(0.0);
        offerDTO.setBonusAmount(0.0);
        offerDTO.setOtherAllowances(0.0);
        offerDTO.setProvidentFund(0.0);
        offerDTO.setProfessionalTax(0.0);
        offerDTO.setIncomeTax(0.0);
        offerDTO.setOtherDeductions(0.0);
        offerDTO.setGrossEarnings(0.0);
        offerDTO.setTotalDeductions(0.0);
        offerDTO.setNetSalary(0.0);
        offerDTO.setAnnualSalary(0.0);
        offerDTO.setTotalSalaryPerMonth(0.0);
        offerDTO.setStipend(0.0);
    }
}
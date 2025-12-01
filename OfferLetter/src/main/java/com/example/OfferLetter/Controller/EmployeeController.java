package com.example.OfferLetter.Controller;


import com.example.OfferLetter.Repository.PayrollRepository;
import com.example.OfferLetter.Security.CheckPermission;
import com.example.OfferLetter.Service.SalaryCalculationService;
import com.example.OfferLetter.Dto.DeparmentDto;
import com.example.OfferLetter.Entity.Employee;
import com.example.OfferLetter.Entity.JobDetails;
import com.example.OfferLetter.Service.EmployeeService;
import com.example.OfferLetter.Service.JobDetailsService;
import com.example.OfferLetter.Entity.Payroll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payroll/jobdetails")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private JobDetailsService jobDetailsService;

    @Autowired
    private SalaryCalculationService salaryCalculationService;

    @Autowired
    private PayrollRepository payrollRepository;


    @CheckPermission(value = "CREATE_PAYROLL")
    @PostMapping("/create")
    public ResponseEntity<?> createEmployeeWithJobDetails(@RequestBody DeparmentDto request) {
        try {
            Employee employee = new Employee();
            employee.setEmployeeId(request.getEmployeeId());
            employee.setEmpName(request.getEmpName());
            employee.setEmail(request.getEmail());
            employee.setPhoneNumber(request.getPhoneNumber());
            employee.setDepartment(request.getDepartment());
            employee.setDesignation(request.getDesignation());
            employee.setAnnualSalary(request.getAnnualSalary());
            employee.setAccountNumber(request.getAccountNumber());
            employee.setIfsccode(request.getIfsccode());
            employee.setBankName(request.getBankName());
            employee.setPfnum(request.getPfnum());
            employee.setPanNumber(request.getPanNumber());
            employee.setAadharNumber(request.getAadharNumber());
            employee.setUanNumber(request.getUanNumber());
            employee.setLevel(request.getLevel());
            employee.setJobType(request.getJobType());
            employee.setStartDate(request.getStartDate());

            // Handle stipend and monthly salary based on job type
            if ("intern".equalsIgnoreCase(request.getJobType())) {
                if (request.getStipend() == null || request.getStipend() <= 0) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Stipend is required for intern job type");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                employee.setStipend(request.getStipend());
                // For interns, monthly salary should be calculated from annual salary OR set to 0
                if (request.getAnnualSalary() != null && request.getAnnualSalary() > 0) {
                    // Calculate monthly salary from annual salary
                    double monthlySalary = (request.getAnnualSalary() * 100000) / 12.0;
                    employee.setMonthlySalary(monthlySalary);
                } else {
                    // If no annual salary, set monthly salary to 0 for interns
                    employee.setMonthlySalary(0.0);
                }
            } else {
                // For non-intern roles
                employee.setStipend(0.0); // Set to 0 instead of null
                if (request.getAnnualSalary() != null && request.getAnnualSalary() > 0) {
                    // Calculate monthly salary from annual salary
                    double monthlySalary = (request.getAnnualSalary() * 100000) / 12.0;
                    employee.setMonthlySalary(monthlySalary);
                } else {
                    employee.setMonthlySalary(0.0);
                }
            }

            Employee savedEmployee = employeeService.saveEmployee(employee);

            JobDetails jobDetails = new JobDetails();
            jobDetails.setJobId(generateJobId(request.getEmployeeId()));
            jobDetails.setDesignation(request.getDesignation());
            jobDetails.setDepartment(request.getDepartment());
            jobDetails.setJobType(request.getJobType());
            jobDetails.setLevel(request.getLevel());
            jobDetails.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());

            // Set stipend in job details as well if needed
            if ("intern".equalsIgnoreCase(request.getJobType())) {
                jobDetails.setStipend(request.getStipend());
            } else {
                jobDetails.setStipend(0.0); // Set to 0 instead of null
            }

            jobDetails.setEmployee(savedEmployee);

            JobDetails savedJobDetails = jobDetailsService.saveJobDetails(jobDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee created successfully with job details");
            response.put("employee", savedEmployee);
            response.put("jobDetails", savedJobDetails);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create employee: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    @GetMapping("/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeeWithJobDetails(@PathVariable String employeeId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Employee employee = employeeService.getEmployeeById(employeeId);
            if (employee == null) {
                response.put("success", false);
                response.put("message", "Employee not found with ID: " + employeeId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            JobDetails latestJobDetails = jobDetailsService.getLatestJobDetailsByEmployeeId(employeeId);
            List<Payroll> payrollList = payrollRepository.findByEmployeeEmployeeId(employeeId);

            // Fixed: Sort payrolls to get the latest one
            Payroll latestPayroll = payrollList.stream()
                    .sorted((p1, p2) -> {
                        int yearCompare = p2.getYear().compareTo(p1.getYear());
                        return yearCompare != 0 ? yearCompare : p2.getMonth().compareTo(p1.getMonth());
                    })
                    .findFirst()
                    .orElse(null);

            Map<String, Object> calculatedSalary = calculateSalaryBreakdown(employee);

            Map<String, Object> data = new HashMap<>();
            data.put("employee", employee);
            data.put("latestJobDetails", latestJobDetails);
            data.put("latestPayroll", latestPayroll);
            data.put("calculatedSalary", calculatedSalary);
            data.put("payrollHistory", payrollList);

            // TEMPORARY: Add verification for debugging
            if (latestPayroll != null) {
                data.put("calculationVerification", verifyPayrollCalculation(latestPayroll));
            }

            response.put("success", true);
            response.put("message", "Employee details retrieved successfully");
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve employee details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // TEMPORARY METHOD - Remove after verification
    private Map<String, Object> verifyPayrollCalculation(Payroll payroll) {
        Map<String, Object> verification = new HashMap<>();

        if (payroll != null) {
            double monthlySalary = payroll.getMonthlySalary() != null ? payroll.getMonthlySalary() : 0;
            double totalDeductions = payroll.getTotalDeductions() != null ? payroll.getTotalDeductions() : 0;
            double expectedNetSalary = monthlySalary - totalDeductions;
            double actualNetSalary = payroll.getNetSalary() != null ? payroll.getNetSalary() : 0;

            verification.put("monthlySalary", monthlySalary);
            verification.put("totalDeductions", totalDeductions);
            verification.put("expectedNetSalary", expectedNetSalary);
            verification.put("actualNetSalary", actualNetSalary);
            verification.put("calculationMatches", Math.abs(expectedNetSalary - actualNetSalary) < 0.01);
            verification.put("difference", Math.abs(expectedNetSalary - actualNetSalary));
        }

        return verification;
    }

    // ADD THIS METHOD to calculate salary breakdown
    private Map<String, Object> calculateSalaryBreakdown(Employee employee) {
        Map<String, Object> salaryData = new HashMap<>();

        if (employee.getAnnualSalary() != null && employee.getAnnualSalary() > 0) {
            try {
                // Convert LPA to rupees (11 LPA = 11,00,000 ₹)
                Double annualSalaryInRupees = employee.getAnnualSalary() * 100000;
                Double monthlySalary = annualSalaryInRupees / 12.0;

                // Calculate salary components (same as your service)
                Double basicSalary = monthlySalary * 0.50;
                Double hraAmount = basicSalary * 0.40;
                Double conveyanceAllowance = 1600.0;
                Double medicalAllowance = 1250.0;
                Double specialAllowance = monthlySalary - (basicSalary + hraAmount + conveyanceAllowance + medicalAllowance);

                // Adjust if special allowance is negative
                if (specialAllowance < 0) {
                    specialAllowance = 0.0;
                    double adjustedAmount = monthlySalary - (hraAmount + conveyanceAllowance + medicalAllowance);
                    if (adjustedAmount > 0) {
                        basicSalary = adjustedAmount;
                    }
                }

                // Calculate deductions
                Double providentFund = basicSalary * 0.12;
                Double professionalTax = 200.0;
                Double incomeTax = calculateMonthlyIncomeTax(annualSalaryInRupees);

                // Calculate totals
                Double grossEarnings = basicSalary + hraAmount + conveyanceAllowance + medicalAllowance + specialAllowance;
                Double totalDeductions = providentFund + professionalTax + incomeTax;
                Double netSalary = grossEarnings - totalDeductions;

                // Populate salary data
                salaryData.put("monthlySalary", monthlySalary);
                salaryData.put("basicSalary", basicSalary);
                salaryData.put("hraAmount", hraAmount);
                salaryData.put("conveyanceAllowance", conveyanceAllowance);
                salaryData.put("medicalAllowance", medicalAllowance);
                salaryData.put("specialAllowance", specialAllowance);
                salaryData.put("bonusAmount", 0.0);
                salaryData.put("otherAllowances", 0.0);
                salaryData.put("providentFund", providentFund);
                salaryData.put("professionalTax", professionalTax);
                salaryData.put("incomeTax", incomeTax);
                salaryData.put("otherDeductions", 0.0);
                salaryData.put("grossEarnings", grossEarnings);
                salaryData.put("totalDeductions", totalDeductions);
                salaryData.put("netSalary", netSalary);
                salaryData.put("annualSalary", annualSalaryInRupees);
                salaryData.put("annualSalaryLPA", employee.getAnnualSalary());

            } catch (Exception e) {
                salaryData.put("error", "Salary calculation failed: " + e.getMessage());
            }
        } else {
            salaryData.put("error", "Annual salary not set for employee");
        }

        return salaryData;
    }

    // ADD THIS METHOD for income tax calculation
    private Double calculateMonthlyIncomeTax(Double annualSalaryInRupees) {
        if (annualSalaryInRupees == null) return 0.0;

        double taxableIncome = annualSalaryInRupees - 50000; // Standard deduction
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
        return Math.max(tax, 0) / 12;
    }

    @GetMapping("/getall")
    public ResponseEntity<?> getAllEmployeesWithJobDetails() {
        try {
            List<Employee> employees = employeeService.getAllEmployees();

            // Simply return the raw entities - they will include stipend due to @JsonProperty
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employees retrieved successfully");
            response.put("data", employees);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve employees: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }



    // Update Employee and Job Details
    @PutMapping("/{employeeId}/update")
    public ResponseEntity<?> updateEmployeeWithJobDetails(
            @PathVariable String employeeId,
            @RequestBody DeparmentDto request) {
        try {

            Employee existingEmployee = employeeService.getEmployeeById(employeeId);
            if (existingEmployee == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Employee not found with ID: " + employeeId);
                return ResponseEntity.notFound().build();
            }

            existingEmployee.setEmpName(request.getEmpName());
            existingEmployee.setEmail(request.getEmail());
            existingEmployee.setPhoneNumber(request.getPhoneNumber());
            existingEmployee.setDepartment(request.getDepartment());
            existingEmployee.setDesignation(request.getDesignation());

            Employee updatedEmployee = employeeService.saveEmployee(existingEmployee);


            JobDetails existingJobDetails = jobDetailsService.getLatestJobDetailsByEmployeeId(employeeId);
            if (existingJobDetails != null) {

                existingJobDetails.setDesignation(request.getDesignation());
                existingJobDetails.setDepartment(request.getDepartment());
                existingJobDetails.setJobType(request.getJobType());
                existingJobDetails.setLevel(request.getLevel());
                if (request.getStartDate() != null) {
                    existingJobDetails.setStartDate(request.getStartDate());
                }
            } else {

                existingJobDetails = new JobDetails();
                existingJobDetails.setJobId(generateJobId(employeeId));
                existingJobDetails.setDesignation(request.getDesignation());
                existingJobDetails.setDepartment(request.getDepartment());
                existingJobDetails.setJobType(request.getJobType());
                existingJobDetails.setLevel(request.getLevel());
                existingJobDetails.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
                existingJobDetails.setEmployee(updatedEmployee);
            }

            JobDetails updatedJobDetails = jobDetailsService.saveJobDetails(existingJobDetails);


            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee updated successfully");
            response.put("employee", updatedEmployee);
            response.put("jobDetails", updatedJobDetails);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update employee: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Delete Employee
    @DeleteMapping("/{employeeId}/delete")
    public ResponseEntity<?> deleteEmployee(@PathVariable String employeeId) {
        try {
            employeeService.deleteEmployee(employeeId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete employee: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    private String generateJobId(String employeeId) {
        return "JOB_" + employeeId + "_" + System.currentTimeMillis();
    }


}

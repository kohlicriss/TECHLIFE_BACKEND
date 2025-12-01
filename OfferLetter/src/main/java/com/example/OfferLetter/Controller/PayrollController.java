package com.example.OfferLetter.Controller;


import com.example.OfferLetter.Security.CheckPermission;
import com.example.OfferLetter.Dto.*;
import com.example.OfferLetter.Entity.Attendance;
import com.example.OfferLetter.Entity.Employee;
import com.example.OfferLetter.Entity.Payroll;
import com.example.OfferLetter.Repository.AttendanceRepository;
import com.example.OfferLetter.Repository.EmployeeRepository;
import com.example.OfferLetter.Repository.PayrollRepository;
import com.example.OfferLetter.Service.SalaryCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/payroll")

public class PayrollController {

    @Value("${ATTENDANCE_PASSWORD}")
    private String ATTENDANCE_PASSWORD ;
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private SalaryCalculationService salaryCalculationService;

    @CheckPermission("CREATE_PAYROLL")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createEmployee(@RequestBody EmployeeRequest request) {
        try {
            // Check if employee already exists
            if (employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Employee with ID " + request.getEmployeeId() + " already exists"));
            }

            Employee employee = new Employee();
            employee.setEmployeeId(request.getEmployeeId());
            employee.setEmpName(request.getEmpName());
            employee.setEmail(request.getEmail());
            employee.setPhoneNumber(request.getPhoneNumber());
            employee.setAnnualSalary(request.getAnnualSalary());
            employee.calculateMonthlySalary();
            employee.setAccountNumber(request.getAccountNumber());
            employee.setIfsccode(request.getIfsccode());
            employee.setBankName(request.getBankName());
            employee.setPfnum(request.getPfnum());
            employee.setPanNumber(request.getPanNumber());
            employee.setAadharNumber(request.getAadharNumber());
            employee.setUanNumber(request.getUanNumber());
            employee.setJobType(request.getJobType());
            employee.setLevel(request.getLevel());
            employee.setStartDate(request.getStartDate());
            employee.setDepartment(request.getDepartment());
            employee.setStipend(request.getStipend());
            employee.setDesignation(request.getDesignation());

            Employee savedEmployee = employeeRepository.save(employee);

            return ResponseEntity.ok(new ApiResponse(true, "Employee created successfully", savedEmployee));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error creating employee: " + e.getMessage()));
        }
    }

    //get employee with specific id
    @GetMapping("/employee/{employeeId}")
    // @CheckPermission(
    //         value = "GET_PAYROLL_MONTHLY",
    //         MatchParmName = "employeeId",
    //         MatchParmFromUrl = "employeeId",
    //         MatchParmForRoles = {"ROLE_ADMIN" , "ROLE_MANAGER" , "ROLE_HR" , "ROLE_EMPLOYEE" , "ROLE_TEAM_LEAD"}
    // )
    public ResponseEntity<ApiResponse> getEmployee(@PathVariable String employeeId) {
        try {
            Optional<Employee> employee = employeeRepository.findByEmployeeId(employeeId);
            if (employee.isPresent()) {
                return ResponseEntity.ok(new ApiResponse(true, "Employee found", employee.get()));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Employee not found with ID: " + employeeId));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching employee: " + e.getMessage()));
        }
    }

    @GetMapping("/offerletter/employee/{employeeId}")
    public ResponseEntity<ApiResponse> getOfferLetterData(@PathVariable String employeeId) {
        try {
            Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);

            if (employeeOpt.isPresent()) {
                Employee employee = employeeOpt.get();

                OfferDTO offerDTO = mapEmployeeToOfferDTO(employee);

                return ResponseEntity.ok(new ApiResponse(true, "Offer letter data found", offerDTO));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Employee not found with ID: " + employeeId));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching offer letter data: " + e.getMessage()));
        }
    }


    private OfferDTO mapEmployeeToOfferDTO(Employee employee) {
        OfferDTO offerDTO = new OfferDTO();

        offerDTO.setEmployeeId(employee.getEmployeeId());
        offerDTO.setEmpName(employee.getEmpName());
        offerDTO.setJobType(employee.getJobType());
        offerDTO.setDepartment(employee.getDepartment());
        offerDTO.setDesignation(employee.getDesignation());
        offerDTO.setAnnualSalary(employee.getAnnualSalary());
        offerDTO.setStipend(employee.getStipend());
        offerDTO.setStartDate(employee.getStartDate());

        return offerDTO;
    }

    // @CheckPermission(
    //         value = "EDIT_BONUS"
    // )
    @PutMapping("/employee/{employeeId}/update")
    public ResponseEntity<ApiResponse> updateEmployee(
            @PathVariable String employeeId,
            @RequestBody EmployeeUpdateDTO employeeUpdateDTO) {
        try {
            // Find the existing employee
            Optional<Employee> existingEmployeeOpt = employeeRepository.findByEmployeeId(employeeId);

            if (existingEmployeeOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Employee not found with ID: " + employeeId));
            }

            Employee existingEmployee = existingEmployeeOpt.get();

            // Update the employee fields with new values
            updateEmployeeFields(existingEmployee, employeeUpdateDTO);

            // Save the updated employee
            Employee updatedEmployee = employeeRepository.save(existingEmployee);

            return ResponseEntity.ok(new ApiResponse(true, "Employee updated successfully", updatedEmployee));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error updating employee: " + e.getMessage()));
        }
    }

    // Helper method to update employee fields
    private void updateEmployeeFields(Employee employee, EmployeeUpdateDTO updateDTO) {
        if (updateDTO.getEmpName() != null) {
            employee.setEmpName(updateDTO.getEmpName());
        }
        if (updateDTO.getEmail() != null) {
            employee.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getPhoneNumber() != null) {
            employee.setPhoneNumber(updateDTO.getPhoneNumber());
        }
        if (updateDTO.getAccountNumber() != null) {
            employee.setAccountNumber(updateDTO.getAccountNumber());
        }
        if (updateDTO.getBankName() != null) {
            employee.setBankName(updateDTO.getBankName());
        }
        if (updateDTO.getPanNumber() != null) {
            employee.setPanNumber(updateDTO.getPanNumber());
        }
        if (updateDTO.getAadharNumber() != null) {
            employee.setAadharNumber(updateDTO.getAadharNumber());
        }
        if (updateDTO.getIfsccode() != null) {
            employee.setIfsccode(updateDTO.getIfsccode());
        }
        if (updateDTO.getPfnum() != null) {
            employee.setPfnum(updateDTO.getPfnum());
        }
        if (updateDTO.getUanNumber() != null) {
            employee.setUanNumber(updateDTO.getUanNumber());
        }
        if (updateDTO.getDepartment() != null) {
            employee.setDepartment(updateDTO.getDepartment());
        }
        if (updateDTO.getDesignation() != null) {
            employee.setDesignation(updateDTO.getDesignation());
        }
    }

    //get all employees
    @GetMapping("/employee")
    public ResponseEntity<ApiResponse> getAllEmployees() {
        try {
            List<Employee> employees = employeeRepository.findAll();
            return ResponseEntity.ok(new ApiResponse(true, "Employees retrieved successfully", employees));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching employees: " + e.getMessage()));
        }
    }

  //post attendance
  //salary recalculating based on payroll id
    @PostMapping("/payroll/recalculate/{payrollId}")
    public ResponseEntity<ApiResponse> recalculateSalary(@PathVariable Long payrollId) {
        try {
            Payroll payroll = salaryCalculationService.recalculateSalary(payrollId);
            return ResponseEntity.ok(new ApiResponse(true, "Salary recalculated successfully", payroll));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error recalculating salary: " + e.getMessage()));
        }
    }

  //get payroll by employee id (employee)
    @GetMapping("/payroll/employee/{employeeId}")
    public ResponseEntity<ApiResponse> getEmployeePayrolls(@PathVariable String employeeId) {
        try {
            List<Payroll> payrolls = payrollRepository.findByEmployeeEmployeeId(employeeId);


            List<PayrollDTO> payrollDTOs = payrolls.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Payroll records retrieved successfully", payrollDTOs));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching payroll records: " + e.getMessage()));
        }
    }

    private PayrollDTO convertToDTO(Payroll payroll) {
        PayrollDTO dto = new PayrollDTO();
        dto.setPayrollId(payroll.getPayrollId());
        dto.setMonth(payroll.getMonth());
        dto.setYear(payroll.getYear());
        dto.setPayDate(payroll.getPayDate());
        dto.setTotalWorkingDays(payroll.getTotalWorkingDays());
        dto.setDaysPresent(payroll.getDaysPresent());
        dto.setPaidDays(payroll.getPaidDays());
        dto.setLossOfPayDays(payroll.getLossOfPayDays());
        dto.setBasicSalary(payroll.getBasicSalary());
        dto.setHraAmount(payroll.getHraAmount());
        dto.setConveyanceAllowance(payroll.getConveyanceAllowance());
        dto.setMedicalAllowance(payroll.getMedicalAllowance());
        dto.setSpecialAllowance(payroll.getSpecialAllowance());
        dto.setBonusAmount(payroll.getBonusAmount());
        dto.setOtherAllowances(payroll.getOtherAllowances());
        dto.setProvidentFund(payroll.getProvidentFund());
        dto.setProfessionalTax(payroll.getProfessionalTax());
        dto.setIncomeTax(payroll.getIncomeTax());
        dto.setOtherDeductions(payroll.getOtherDeductions());
        dto.setGrossEarnings(payroll.getGrossEarnings());
        dto.setTotalDeductions(payroll.getTotalDeductions());
        dto.setNetSalary(payroll.getNetSalary());
        dto.setStatus(payroll.getStatus());

        dto.setStipend(payroll.getStipend());

//        // Convert employee to DTO
//        Employee employee = payroll.getEmployee();
//        EmployeeDTO employeeDTO = new EmployeeDTO();
//        employeeDTO.setEmployeeId(employee.getEmployeeId());
//        employeeDTO.setEmpName(employee.getEmpName());
//        employeeDTO.setEmail(employee.getEmail());
//        employeeDTO.setPhoneNumber(employee.getPhoneNumber());
//        employeeDTO.setAnnualSalary(employee.getAnnualSalary());
//        employeeDTO.setMonthlySalary(employee.getMonthlySalary());
//        employeeDTO.setAccountNumber(employee.getAccountNumber());
//        employeeDTO.setBankName(employee.getBankName());
//        employeeDTO.setPanNumber(employee.getPanNumber());
//        employeeDTO.setAadharNumber(employee.getAadharNumber());
//        employeeDTO.setUanNumber(employee.getUanNumber());
//        employeeDTO.setPfDeductionRate(employee.getPfDeductionRate());
//        employeeDTO.setProfessionalTax(employee.getProfessionalTax());
//
//        dto.setEmployee(employeeDTO);

        return dto;
    }
  //get by payroll id
    @GetMapping("/payroll/{payrollId}")
    public ResponseEntity<ApiResponse> getPayroll(@PathVariable Long payrollId) {
        try {
            Optional<Payroll> payroll = payrollRepository.findById(payrollId);
            if (payroll.isPresent()) {
                return ResponseEntity.ok(new ApiResponse(true, "Payroll record found", payroll.get()));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Payroll record not found with ID: " + payrollId));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching payroll record: " + e.getMessage()));
        }
    }

    // Get employee salary employee ID (every month) (summary)
    @GetMapping("/employees/{employeeId}/salary")
    public ResponseEntity<ApiResponse> getEmployeeSalarySummary(@PathVariable String employeeId) {
        try {
            List<Payroll> payrolls = payrollRepository.findByEmployeeEmployeeId(employeeId);

            if (payrolls.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "No payroll records found for employee: " + employeeId));
            }

            List<EmployeeSalarySummaryDTO> summaries = payrolls.stream()
                    .map(this::convertToEmployeeSalarySummary)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Employee salary summaries retrieved successfully", summaries));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching employee salary summaries: " + e.getMessage()));
        }
    }


    @GetMapping("/employees/{employeeId}/salary/{month}/{year}")
    public ResponseEntity<ApiResponse> getEmployeeSalarySummaryByMonth(
            @PathVariable String employeeId,
            @PathVariable Integer month,
            @PathVariable Integer year) {
        try {
            Optional<Payroll> payroll = payrollRepository.findByEmployeeEmployeeIdAndMonthAndYear(employeeId, month, year);

            if (payroll.isPresent()) {
                EmployeeSalarySummaryDTO summary = convertToEmployeeSalarySummary(payroll.get());
                return ResponseEntity.ok(new ApiResponse(true, "Employee salary summary retrieved successfully", summary));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "No payroll record found for employee " + employeeId + " for " + month + " " + year));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching employee salary summary: " + e.getMessage()));
        }
    }


    //get salries of all employee for specific month and year
    @CheckPermission(
            value = "GET_PAYROLL_MONTHLY"
    )
    @GetMapping("/payroll/salary/all/{month}/{year}")
    public ResponseEntity<ApiResponse> getAllEmployeesSalarySummaries(
            @PathVariable Integer month,
            @PathVariable Integer year) {
        try {

            List<Payroll> payrolls = payrollRepository.findByMonthAndYear(month, year);
            //List<Payroll> payrolls = payrollRepository.findByMonthAndYear(monthString, year);

            if (payrolls.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "No payroll records found for " + month + " " + year));
            }

            List<EmployeeSalarySummaryDTO> summaries = payrolls.stream()
                    .map(this::convertToEmployeeSalarySummary)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Salary summaries retrieved successfully", summaries));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching salary summaries: " + e.getMessage()));
        }
    }

    private EmployeeSalarySummaryDTO convertToEmployeeSalarySummary(Payroll payroll) {
        EmployeeSalarySummaryDTO dto = new EmployeeSalarySummaryDTO();

        // Set the payrollId from the Payroll entity's ID
        dto.setPayrollId(payroll.getPayrollId());
        dto.setEmployeeId(payroll.getEmployee().getEmployeeId());
        dto.setEmployeeName(payroll.getEmployee().getEmpName());
        dto.setMonth(payroll.getMonth());
        dto.setYear(payroll.getYear());

        dto.setConveyanceAllowance(payroll.getConveyanceAllowance());
        dto.setDaysPresent(payroll.getDaysPresent());
        dto.setHraAmount(payroll.getHraAmount());
        dto.setLossOfPayDays(payroll.getLossOfPayDays());
        dto.setGrossEarnings(payroll.getGrossEarnings());
        dto.setOtherAllowances(payroll.getOtherAllowances());
        dto.setBasicSalary(payroll.getBasicSalary());
        dto.setSpecialAllowance(payroll.getSpecialAllowance());
        dto.setMedicalAllowance(payroll.getMedicalAllowance());

        dto.setProfessionalTax(payroll.getProfessionalTax());
        dto.setProvidentFund(payroll.getProvidentFund());
        dto.setIncomeTax(payroll.getIncomeTax());
        dto.setAttendanceDeduction(payroll.getAttendanceDeduction());
        dto.setPaidDays(payroll.getPaidDays());
        dto.setTotalWorkingDays(payroll.getTotalWorkingDays());
        dto.setPfnum(payroll.getEmployee().getPfnum());
        dto.setIfsccode(payroll.getEmployee().getIfsccode());
        dto.setPanNumber(payroll.getEmployee().getPanNumber());
        dto.setDesignation(payroll.getEmployee().getDesignation());

        // Set bonus amount
        dto.setBonusAmount(payroll.getBonusAmount());

        dto.setTotalSalary(payroll.getBasicSalary() + payroll.getHraAmount() + payroll.getBonusAmount()+ payroll.getConveyanceAllowance()
                + payroll.getMedicalAllowance() + payroll.getSpecialAllowance() + payroll.getOtherAllowances());

        // Calculate total deductions
        Double totalDeductions = payroll.getProvidentFund() + payroll.getProfessionalTax()
                + payroll.getIncomeTax() + payroll.getOtherDeductions()+payroll.getAttendanceDeduction();
        dto.setTotalDeductions(totalDeductions);

        // Set net salary
        dto.setNetSalary(payroll.getNetSalary());

        return dto;

    }
    

    //bonous update
    @PutMapping("/admin/payroll/{payrollId}/update")
    public ResponseEntity<ApiResponse> updatePayrollComponents(
            @PathVariable Long payrollId,
            @RequestBody UpdatePayrollRequest request) {
        try {
            // Validate bonus amount if provided
            if (request.getBonusAmount() != null && request.getBonusAmount() < 0) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Bonus amount cannot be negative"));
            }

            Payroll payroll = salaryCalculationService.updatePayrollComponents(payrollId, request);
            EmployeeSalarySummaryDTO summaryDTO = convertToEmployeeSalarySummary(payroll);

            return ResponseEntity.ok(new ApiResponse(true, "Payroll components updated successfully", summaryDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error updating payroll components: " + e.getMessage()));
        }
    }



//get salary detials of everymonth of a particular employee
    @GetMapping("/employees/fulldata/{employeeId}")
    public ResponseEntity<ApiResponse> getEmployeeFullDetails(@PathVariable String employeeId) {
        try {
            Optional<Employee> employee = employeeRepository.findByEmployeeId(employeeId);
            if (employee.isPresent()) {
                List<Attendance> attendanceRecords = attendanceRepository.findByEmployeeEmployeeId(employeeId);
                List<Payroll> payrolls = payrollRepository.findByEmployeeEmployeeId(employeeId);

                EmployeeFullDetailsResponse response = new EmployeeFullDetailsResponse();
                response.setEmployee(employee.get());
                response.setAttendanceRecords(attendanceRecords);
                response.setPayrolls(payrolls);

                return ResponseEntity.ok(new ApiResponse(true, "Employee details retrieved successfully", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Employee not found with ID: " + employeeId));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching employee details: " + e.getMessage()));
        }
    }
}



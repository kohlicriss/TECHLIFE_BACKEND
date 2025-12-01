package com.example.OfferLetter.Controller;

import com.example.OfferLetter.Repository.AttendanceRepository;
import com.example.OfferLetter.Repository.EmployeeRepository;
import com.example.OfferLetter.Repository.PayrollRepository;
import com.example.OfferLetter.Service.SalaryCalculationService;
import com.example.OfferLetter.Dto.ApiResponse;
import com.example.OfferLetter.Dto.AttendanceRequest;
import com.example.OfferLetter.Entity.Attendance;
import com.example.OfferLetter.Entity.Employee;
import com.example.OfferLetter.Entity.Payroll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/attendance")
public class AttendancePost {
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
    @PostMapping("/add")
    public ResponseEntity<ApiResponse> createAttendance(@RequestBody AttendanceRequest request , @RequestHeader String PassKey) {

        System.out.println(ATTENDANCE_PASSWORD);
        System.out.println(PassKey);

        if(!ATTENDANCE_PASSWORD.equals(PassKey)){
            throw new RuntimeException("UnAuthorized Exception" );
        }
        try {
            // Validate employee exists
            Employee employee = employeeRepository.findByEmployeeId(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + request.getEmployeeId()));

            // Check if attendance already exists
            Optional<Attendance> existingAttendance = attendanceRepository
                    .findByEmployeeAndMonthYear(request.getEmployeeId(), request.getMonth(), request.getYear());

            if (existingAttendance.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Attendance record already exists for " + request.getMonth() + " " + request.getYear()));
            }

            // Create and save attendance
            Attendance attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setMonth(request.getMonth());
            attendance.setYear(request.getYear());
            attendance.setTotalWorkingDays(request.getTotalWorkingDays());
            attendance.setDaysPresent(request.getDaysPresent());
            attendance.setUnpaidLeaves(request.getUnpaidLeaves());
            attendance.setCreatedDate(LocalDate.now());

            Attendance savedAttendance = attendanceRepository.save(attendance);

            // AUTO-GENERATE PAYSLIP AFTER ATTENDANCE
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("attendance", savedAttendance);

            try {
                Payroll payroll = salaryCalculationService.calculateSalary(
                        request.getEmployeeId(),
                        request.getMonth(),
                        request.getYear()
                );
                responseData.put("payroll", payroll);

                return ResponseEntity.ok(new ApiResponse(true,
                        "Attendance record created and payslip generated successfully",
                        responseData));

            } catch (Exception payrollError) {
                System.err.println("Payroll generation failed: " + payrollError.getMessage());
                return ResponseEntity.ok(new ApiResponse(true,
                        "Attendance recorded but payroll generation failed: " + payrollError.getMessage(),
                        responseData));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error creating attendance record: " + e.getMessage()));
        }
    }

}

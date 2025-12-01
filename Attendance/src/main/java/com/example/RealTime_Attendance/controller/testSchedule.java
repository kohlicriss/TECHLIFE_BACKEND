package com.example.RealTime_Attendance.controller;

import com.example.RealTime_Attendance.Client.PayRoll;
import com.example.RealTime_Attendance.Dto.AttendanceRequest;
import com.example.RealTime_Attendance.Entity.PersonalLeaves;
import com.example.RealTime_Attendance.repository.AttendanceRepository;
import com.example.RealTime_Attendance.repository.PersonalLeavesRepository;
import com.example.RealTime_Attendance.service.PersonalLeavesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(path = "/api/attendance/test/")
@Slf4j
public class testSchedule {
    @Value("${PAYROLL_PASSWARD}")
    private String PAYROLL_PASSWARD;

    @Autowired
    private PersonalLeavesService personalLeavesService;

    @Autowired
    private PersonalLeavesRepository personalLeavesRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private PayRoll payRoll;


    @PutMapping("/InitializeMonthlyLeaves")
    public void initializeMonthlyLeaveBalances() {
        YearMonth current = YearMonth.now();
        Integer month = (Integer) current.getMonthValue();
        Integer year = (Integer) current.getYear();

        List<String> employeeIds = attendanceRepository.findAllEmployeeIds();

        for (String empId : employeeIds) {
            boolean exists = personalLeavesRepository
                    .existsByEmployeeIdAndMonthAndYear(empId, month, year);

            if (!exists) {
                PersonalLeaves newLeave = PersonalLeaves.builder()
                        .employeeId(empId)
                        .month(month)
                        .year(year)
                        .casual(2.0)
                        .paid(1.0)
                        .sick(2.0)
                        .unpaid(0.0)
                        .casualConsumed(0.0)
                        .paidConsumed(0.0)
                        .sickConsumed(0.0)
                        .unpaidConsumed(0.0)
                        .build();

                personalLeavesRepository.save(newLeave);
            }
        }

        System.out.println("Leave balances initialized for month: " + month + ", year: " + year);
    }

    @PutMapping("/payrollPosting")
    public String genrateAndPostAttendanceReport(){
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        CompletableFuture<List<AttendanceRequest>> report = personalLeavesService.attendanceReport(month, year, 0, Integer.MAX_VALUE);
        report.thenAcceptAsync(attendanceRequests -> {
            attendanceRequests.forEach(request -> {
                try {
                    System.out.println(PAYROLL_PASSWARD);
                    System.out.println(request);
                    payRoll.generatePaySlips(request, PAYROLL_PASSWARD);
                } catch (Exception e) {
                    log.error("Error sending to payroll for {}", request.getEmployeeId(), e);
                }
            });
        });
        return "Posted successfully";
    }
}


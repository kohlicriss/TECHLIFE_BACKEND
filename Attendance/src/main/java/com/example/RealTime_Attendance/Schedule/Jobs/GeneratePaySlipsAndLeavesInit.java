package com.example.RealTime_Attendance.Schedule.Jobs;

import com.example.RealTime_Attendance.Client.PayRoll;
import com.example.RealTime_Attendance.Dto.AttendanceRequest;
import com.example.RealTime_Attendance.Entity.PersonalLeaves;
import com.example.RealTime_Attendance.repository.AttendanceRepository;
import com.example.RealTime_Attendance.repository.PersonalLeavesRepository;
import com.example.RealTime_Attendance.service.PersonalLeavesService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@NoArgsConstructor
@Slf4j
@Component
public class GeneratePaySlipsAndLeavesInit implements Job {
    @Value("${PAYROLL_PASSWARD}")
    private String PAYROLL_PASSWARD;
    @Autowired
    AttendanceRepository attendanceRepository;
    @Autowired
    PersonalLeavesRepository personalLeavesRepository;
    @Autowired
    PersonalLeavesService personalLeavesService;

    @Autowired
    private PayRoll payRoll;

    @Override
    public void execute(JobExecutionContext context){
        System.out.println("triggered Monthly job");
        genrateAndPostAttendanceReport();
        initializeMonthlyLeaveBalances();
    }

    public void initializeMonthlyLeaveBalances() {
        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);
        Integer month = (Integer) current.getMonthValue();
        Integer year = (Integer) current.getYear();

        List<String> employeeIds = attendanceRepository.findAllEmployeeIds();

        for (String empId : employeeIds) {
            boolean exists = personalLeavesRepository
                    .existsByEmployeeIdAndMonthAndYear(empId, month, year);

            Optional<PersonalLeaves> previousMonth = personalLeavesRepository.findByEmployeeIdAndMonthAndYear(empId , (Integer) previous.getMonthValue(), (Integer) previous.getYear());

            if (!exists) {
                PersonalLeaves newLeave = PersonalLeaves.builder()
                        .employeeId(empId)
                        .month(month)
                        .year(year)
                        .casual(previousMonth.get().getCasual() != null ? previousMonth.get().getCasual() : 0)
                        .paid(previousMonth.get().getPaid() != null ? previousMonth.get().getPaid() : 0)
                        .sick(previousMonth.get().getSick() != null ? previousMonth.get().getSick() : 0)
                        .unpaid(previousMonth.get().getUnpaid() != null ? previousMonth.get().getUnpaid() : 0)
                        .casualConsumed(0.0)
                        .paidConsumed(0.0)
                        .sickConsumed(0.0)
                        .unpaidConsumed(0.0)
                        .shifts(previousMonth.get().getShifts())
                        .latitude(previousMonth.get().getLatitude() != null ? previousMonth.get().getLatitude() : 0)
                        .longitude(previousMonth.get().getLongitude() != null ? previousMonth.get().getLongitude() : 0)
                        .timezone(previousMonth.get().getTimezone()!=null ? previousMonth.get().getTimezone() : "Asia/Kolkata")
                        .build();

                personalLeavesRepository.save(newLeave);
            }
        }
    }
    public void genrateAndPostAttendanceReport(){
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        CompletableFuture<List<AttendanceRequest>> report = personalLeavesService.attendanceReport(month, year, 0, Integer.MAX_VALUE);
        report.thenAcceptAsync(attendanceRequests -> {
            attendanceRequests.forEach(request -> {
                try {
                    payRoll.generatePaySlips(request, PAYROLL_PASSWARD);
                } catch (Exception e) {
                    log.error("Error sending to payroll for {}", request.getEmployeeId(), e);
                }
            });
        });
    }
}

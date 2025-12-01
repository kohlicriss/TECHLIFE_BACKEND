package com.example.RealTime_Attendance.Schedule.Jobs;


import com.example.RealTime_Attendance.Entity.*;
import com.example.RealTime_Attendance.Enums.Section;
import com.example.RealTime_Attendance.Exception.CustomException;
import com.example.RealTime_Attendance.repository.*;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping(path = "/api/attendance/test/")
@Slf4j
@NoArgsConstructor
public class AttendanceCheck implements Job {
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private ShiftsRepo shiftsRepository;
    @Autowired
    private PersonalLeavesRepository personalLeavesRepository;


    @Override
    public void execute(JobExecutionContext context) {
        String shiftName = context.getJobDetail().getJobDataMap().getString("shiftName");
        String section =  context.getJobDetail().getJobDataMap().getString("section");
        Section sectionEnum = Section.valueOf(section.toUpperCase());
        String zone = context.getJobDetail().getJobDataMap().getString("zone");
        System.out.println("Shift Name: " + shiftName + " Section: " + sectionEnum);
        markAbsent(LocalDate.now(ZoneId.of(zone)), sectionEnum, shiftName);
    }

    public String markAbsent(LocalDate date, Section section, String shiftName) {

        Shifts shift = shiftsRepository.findById(shiftName)
                .orElseThrow(() -> new CustomException("Shift not found: " + shiftName, HttpStatus.NOT_FOUND));

        LocalTime shiftStartTime = shift.getStartTime();
        LocalTime halfTime = shift.getHalfTime(); // Needed for second section
        Duration checkAfter = shift.getTakeAttendanceAfter();
        LocalDateTime oneHourAfterShiftStart = LocalDateTime.of(date, shiftStartTime).plus(checkAfter);
        LocalDateTime oneHourAfterHalfTime = LocalDateTime.of(date, halfTime).plus(checkAfter);
        System.out.println("Attendance Checking on times "+ oneHourAfterHalfTime + " " + oneHourAfterShiftStart);
        List<PersonalLeaves> employees = personalLeavesRepository.findByShifts(shift);
        int absentCount = 0;

        for (PersonalLeaves emp : employees) {
            String employeeId = emp.getEmployeeId();

            Optional<Attendance> attendanceOpt = attendanceRepository.findByEmployeeIdAndDate(employeeId, date);
            Attendance attendance;

            if (attendanceOpt.isPresent()) {
                attendance = attendanceOpt.get();

                LocalDateTime loginTime = attendance.getLoginTime();
                boolean markAbsent = false;

                if (section == Section.FIRST) {
                    // Absent if no login or logged in after 1 hr from shift start
                    markAbsent = (loginTime == null || loginTime.isAfter(oneHourAfterShiftStart));
                    System.out.println("Marking Absent For first Section " + markAbsent + "Login time is " + loginTime + " " + "For employee " + employeeId);
                    if (!markAbsent) attendance.setFirstSection(true);
                } else {
                    // Absent if no login or logged in after 1 hr from half-time (late for 2nd half)
                    markAbsent = (loginTime == null || loginTime.isAfter(oneHourAfterHalfTime));
                    System.out.println("Marking Absent For second Section " + markAbsent + "Login time is " + loginTime + " " + "For employee " + employeeId);
                    if (!markAbsent) attendance.setSecondSection(true);
                }

                if (markAbsent) {
                    attendance.setAttended(false);
                    absentCount++;
                    attendanceRepository.save(attendance);
                }

            } else {
                // No attendance record exists — create one
                attendance = new Attendance();
                attendance.setEmployeeId(employeeId);
                attendance.setDate(date);
                attendance.setMonth(String.valueOf(date.getMonth()));
                attendance.setYear(date.getYear());
                attendance.setAttendanceId(employeeId + "_" + date);
                attendance.setMode("Auto-Absent");
                attendance.setAttended(false);

                if (section == Section.FIRST) {
                    attendance.setFirstSection(false);
                } else {
                    attendance.setSecondSection(false);
                }

                attendanceRepository.save(attendance);
                absentCount++;
            }
        }

        return "Marked " + absentCount + " employees absent for " + section + " on " + date + " in shift " + shiftName;
    }
}

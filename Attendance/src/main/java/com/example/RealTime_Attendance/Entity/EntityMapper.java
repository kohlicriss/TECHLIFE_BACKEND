package com.example.RealTime_Attendance.Entity;

import com.example.RealTime_Attendance.Dto.AttendanceRequest;
import com.example.RealTime_Attendance.Enums.WorkingDaysType;
import com.example.RealTime_Attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityMapper {

    @Autowired
    AttendanceService attendanceService;

    public AttendanceRequest toAttendanceRequest(PersonalLeaves person){
        return AttendanceRequest.builder()
                .employeeId(person.getEmployeeId())
                .month(person.getMonth() != null ? person.getMonth() : null)
                .year(person.getYear() != null ? person.getYear() : null)
                .totalWorkingDays(attendanceService.getWorkingDays(
                        person.getEmployeeId(), person.getMonth(), person.getYear(), WorkingDaysType.TOTAL))
                .daysPresent(attendanceService.getWorkingDays(
                        person.getEmployeeId(), person.getMonth(), person.getYear(), WorkingDaysType.PRESENT))
                .unpaidLeaves(person.getUnpaidConsumed() != null ?  person.getUnpaidConsumed().floatValue() : 0)
                .build();
    }
}

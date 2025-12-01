package com.example.RealTime_Attendance.Dto;


import com.example.RealTime_Attendance.Enums.Section;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.CronExpression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleAttendance {
    private String Shift;
    private Section Section;
    private String CronExpression;
    private String Zone;
}

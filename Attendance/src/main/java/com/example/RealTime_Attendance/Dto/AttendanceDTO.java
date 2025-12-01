package com.example.RealTime_Attendance.Dto;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String employee_id;
    private String date;
    private String login_time;
    private String logout_time;
}

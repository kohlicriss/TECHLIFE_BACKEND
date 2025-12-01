package com.example.RealTime_Attendance.Dto;

import lombok.*;
import java.time.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarDto {
    private Long id;
    private LocalDate date;
    private String employeeId;
    private Duration effectiveHours;
    private String isPresent;
    private String event;
    private String description;
    private String day;
    private boolean holiday;
    private LocalTime login;
    private LocalTime logout;
    private String mode;
    private Duration grossHours;
    private Boolean firstSection;
    private Boolean secondSection;
    private Boolean considerPresent;
}


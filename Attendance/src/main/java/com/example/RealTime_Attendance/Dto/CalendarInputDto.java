package com.example.RealTime_Attendance.Dto;

import java.time.LocalDate;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarInputDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private String event;
    private String description;
    private boolean holiday;
}

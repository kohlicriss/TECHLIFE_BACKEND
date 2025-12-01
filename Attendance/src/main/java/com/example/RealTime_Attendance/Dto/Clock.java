package com.example.RealTime_Attendance.Dto;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clock {
    private String timeZone;
    private String mode;
    private Double latitude;
    private Double longitude;
    private String device;
}

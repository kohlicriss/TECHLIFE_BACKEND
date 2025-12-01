package com.example.RealTime_Attendance.Dto;

import java.time.Duration;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class PersonDetailsAddDto {
    private Long id = null;
    private String employeeId;
    
    private Integer month;
    private Integer year;

    private Double paid;
    private Double sick;
    private Double casual;
    private Double unpaid;

    private Double paidConsumed = 0.0;
    private Double sickConsumed = 0.0;
    private Double casualConsumed = 0.0;
    private Double unpaidConsumed = 0.0;

    private String shiftName;

    private Long weekEffectiveHours = 0L;
    private Long monthlyEffectiveHours = 0L;
    private Long monthlyOnTime = 0L;
    private Duration monthlyOvertime = Duration.ZERO;

    private Double latitude = (double) 0;
    private Double longitude = (double) 0;
    private String timezone = "Asia/Kolkata";

}

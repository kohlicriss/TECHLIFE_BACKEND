package com.example.RealTime_Attendance.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonalLeaveDetailsDTO {
    private String type;
    private Double consumed;
    private Double remaining;
    private Double total;
}

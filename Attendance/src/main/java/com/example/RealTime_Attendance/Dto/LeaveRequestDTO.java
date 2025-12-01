package com.example.RealTime_Attendance.Dto;

import com.example.RealTime_Attendance.Enums.LeaveType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class LeaveRequestDTO {

    @JsonProperty("employeeId")
    private String employeeId;

    @JsonProperty("numberOfDays")
    private Integer numberOfDays;

    @JsonProperty("req_To_from")
    private LocalDate req_To_from;

    @JsonProperty("req_To_to")
    private LocalDate req_To_to;

    @JsonProperty("leave_Reason")
    private String leave_Reason;

    @JsonProperty("isHalf_Day")
    private boolean isHalf_Day;

    @JsonProperty("leave_Type")
    private LeaveType leave_Type;
}

package com.example.RealTime_Attendance.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import com.example.RealTime_Attendance.Enums.LeaveStatus;
import com.example.RealTime_Attendance.Enums.LeaveType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LeavesDto implements Serializable {
    private static final long serialVersionUID = 1L;
    @Builder.Default
    private Long id = null;
    @Builder.Default
    private Long employeeId = null;
    @Builder.Default
    private boolean isHalf_Day = false;
    private LeaveType Leave_type;
    private String Leave_on;
    private LeaveStatus status;
    private String Request_By;
    private String Details;
    private String Action_Date;
    private String Rejection_Reason;
    private String Action;
}

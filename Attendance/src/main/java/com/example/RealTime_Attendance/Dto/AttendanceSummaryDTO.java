package com.example.RealTime_Attendance.Dto;

import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryDTO {
    private LocalDate date;
    private int present;
    private int absent;

    private int paidApprovedLeaves;
    private int paidUnapprovedLeaves;
    private int unpaidApprovedLeaves;
    private int unpaidUnapprovedLeaves;
    private int sickLeaves;
    private int sickUnapprovedLeaves;
    private int casualApprovedLeaves;
    private int casualUnapprovedLeaves;
    private int approvedLeaves;
    private int pendingLeaves;
}

package com.example.RealTime_Attendance.Entity;

import com.example.RealTime_Attendance.Dto.LeavesBarGraphDTO;
import com.example.RealTime_Attendance.Dto.LeavesDto;
import com.example.RealTime_Attendance.Enums.LeaveStatus;
import com.example.RealTime_Attendance.Enums.LeaveType;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

@Entity
@Table(name = "leaves", schema = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeId;
    private LocalDate reqOn;
    private LocalDate reqTo;
    private LeaveStatus status;
    private String leaveReason;

    @Column(name = "is_half_day")
    private boolean isHalfDay;

    private String rejectionReason;
    private LeaveType leaveType;
    private LocalDate approvedOn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Transient
    public LeavesBarGraphDTO toLeavesBarGraphDto() {
        String day = this.getReqTo()
                .getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        return new LeavesBarGraphDTO(day, 1);
    }

    public static LeavesDto fromEntity(LeaveEntity entity) {
        try {
            if (entity == null) {
                throw new IllegalArgumentException("LeaveEntity cannot be null");
            }

            return LeavesDto.builder()
                    .id(entity.getId() != null ? entity.getId() : null)
                    .employeeId(parseEmployeeId(entity.getEmployeeId()))
                    .isHalf_Day(entity.isHalfDay())
                    .Leave_type(entity.getLeaveType() != null ? entity.getLeaveType() : LeaveType.OTHER)
                    .Leave_on(entity.getReqOn() != null ? entity.getReqOn().toString() : null)
                    .status(entity.getStatus() != null ? entity.getStatus() : LeaveStatus.PENDING)
                    .Request_By(entity.getEmployeeId() != null ? entity.getEmployeeId() : "Unknown")
                    .Details(entity.getLeaveReason() != null ? entity.getLeaveReason() : "No details provided")
                    .Action_Date(entity.getApprovedOn() != null ? entity.getApprovedOn().toString() : null)
                    .Rejection_Reason(entity.getRejectionReason() != null ? entity.getRejectionReason() : "N/A")
                    .Action(determineAction(entity))
                    .build();

        } catch (Exception e) {
            System.err.println("Error converting LeaveEntity to LeavesDto: " + e.getMessage());
            e.printStackTrace();

            // Return a default/empty DTO to prevent breaking the flow
            return LeavesDto.builder()
                    .id(null)
                    .employeeId(null)
                    .isHalf_Day(false)
                    .Leave_type(LeaveType.OTHER)
                    .Leave_on(null)
                    .status(LeaveStatus.PENDING)
                    .Request_By("Unknown")
                    .Details("Conversion error: " + e.getMessage())
                    .Action_Date(null)
                    .Rejection_Reason("N/A")
                    .Action("Unknown")
                    .build();
        }
    }

    // Helper to safely parse employeeId (in case it’s not a valid number)
    private static Long parseEmployeeId(String employeeIdStr) {
        try {
            return (employeeIdStr != null && !employeeIdStr.isBlank()) ? Long.parseLong(employeeIdStr) : null;
        } catch (NumberFormatException e) {
            System.err.println("Invalid employeeId format: " + employeeIdStr);
            return null;
        }
    }

    // Helper to determine action based on status
    private static String determineAction(LeaveEntity entity) {
        if (entity.getStatus() == null) {
            return "Pending";
        }

        switch (entity.getStatus()) {
            case APPROVED:
                return "Approved by Admin";
            case REJECTED:
                return "Rejected by Admin";
            case PENDING:
            default:
                return "Waiting for Approval";
        }
    }

}

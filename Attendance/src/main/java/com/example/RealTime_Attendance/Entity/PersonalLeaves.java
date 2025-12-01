package com.example.RealTime_Attendance.Entity;
import com.example.RealTime_Attendance.Dto.AttendanceRequest;
import com.example.RealTime_Attendance.Enums.WorkingDaysType;
import com.example.RealTime_Attendance.service.AttendanceService;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

@Builder
@Entity
@Table(name = "personal_leaves", schema = "attendance")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonalLeaves {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeId;
    
    private Integer month;
    private Integer year;

    private Double paid;
    private Double sick;
    private Double casual;
    private Double unpaid;

    private Double paidConsumed;
    private Double sickConsumed;
    private Double casualConsumed;
    private Double unpaidConsumed;
    @ManyToOne
    @JoinColumn(name = "shifts")
    private Shifts shifts;

    private Long weekEffectiveHours;
    private Long monthlyEffectiveHours;
    private Long monthlyOnTime;
    private Duration monthlyOvertime;

    private Double latitude;
    private Double longitude;
    private String timezone;


}

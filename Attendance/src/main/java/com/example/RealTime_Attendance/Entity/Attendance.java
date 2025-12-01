package com.example.RealTime_Attendance.Entity;

import com.example.RealTime_Attendance.Enums.Section;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDate;
import com.example.RealTime_Attendance.Dto.BarGraphDTO;
import com.example.RealTime_Attendance.Dto.LinegraphAttedance;
import com.example.RealTime_Attendance.Dto.PieChartDTO;
import com.example.RealTime_Attendance.Dto.SimpleAttendanceDto;
import com.example.RealTime_Attendance.service.Helper;
import lombok.extern.slf4j.Slf4j;

import lombok.*;

import java.time.Duration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@ToString(exclude = "breaks") 
@Slf4j
@Table(name = "attendance", schema = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String attendanceId;
    private String employeeId;


    private LocalDate date;
    private String month;
    private Integer year;
    private boolean isAttended = false;
    @Column(name = "first_section")
    private boolean firstSection = false;
    @Column(name = "second_section")
    private boolean secondSection = false;
    @Column(name = "consider_present")
    private boolean considerPresent = false;
    private String timeZone;
    private Double latitude;
    private Double longitude;
    private String device;
    private String mode;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;

    @OneToMany(mappedBy = "attendance", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Break> breaks = new ArrayList<>();

    private Duration effectiveHours = this.calcEffectiveHours();

    public Duration calcGrossHours() {
        try {
            LocalDateTime login=this.getLoginTime();
            LocalDateTime logout=this.getLogoutTime();
            if(login==null) return Duration.ZERO;
            if(logout==null) logout = LocalDateTime.now(ZoneId.of(this.getTimeZone()));
            if(logout.isBefore(login))return Duration.ZERO;
            return Duration.between(login,logout);
        }catch(Exception e){
            e.printStackTrace();
            return Duration.ZERO;
        }
    }

    public Duration calcEffectiveHours() {
        try{
            Duration total=calcGrossHours();
            List<Break> breaks=this.getBreaks();
            System.out.println(total);
            if(breaks!=null){
                for(Break b:breaks){
                    if(b.getStartTime()!=null&&b.getEndTime()!=null){
                        total=total.minus(Duration.between(b.getStartTime(),b.getEndTime()));
                        System.out.println(total);
                        System.out.println(b);
                    }
                }
            }
            return total.isNegative()?Duration.ZERO:total;
        }catch(Exception e){
            e.printStackTrace();
            return Duration.ZERO;
        }
    }



    @PrePersist
    public void generateAttendanceId() {
        if (employeeId != null && date != null) {
            this.attendanceId = employeeId + "_" + date.toString();
        }
    }

    public Duration breakDuration() {
        Duration total = Duration.ZERO;
        if (breaks != null) {
            for (Break br : breaks) {
                LocalDateTime start = br.getStartTime();
                LocalDateTime end = br.getEndTime();
                if (start != null && end != null) {
                    total = total.plus(Duration.between(start, end));
                }
            }
        }
        return total;
    }

    public PieChartDTO toPieChartDto() {
        PieChartDTO dto = new PieChartDTO();
        try {
            dto.setEmployeeId(this.getEmployeeId());
            dto.setDate(String.valueOf(this.getDate().getDayOfMonth()));
            dto.setMonth(this.getMonth());
            dto.setYear(this.getYear());
            Duration effective = this.getEffectiveHours();
            Duration breakDur = this.breakDuration();
            dto.setWorking_hour(effective != null ? Helper.DurationToPercentage(effective) : 0.0);
            dto.setBreak_hour(breakDur != null ? Helper.DurationToPercentage(breakDur) : 0.0);
        } catch (Exception e) {
            dto.setWorking_hour(0.0);
            dto.setBreak_hour(0.0);
        }
        return dto;
    }

    public BarGraphDTO toBarGraphDTO() {
        BarGraphDTO dto = new BarGraphDTO();
        try {
            dto.setEmployeeId(this.getEmployeeId());
            dto.setDate(this.getDate() != null ? String.valueOf(this.getDate().getDayOfMonth()) : null);
            dto.setMonth(this.getMonth());
            dto.setYear(this.getYear() != null ? this.getYear().toString() : null);
            dto.setStart_time(this.getLoginTime() != null ? Helper.localDateTimeToString(this.getLoginTime()) : null);
            dto.setEnd_time(this.getLogoutTime() != null ? Helper.localDateTimeToString(this.getLogoutTime()) : null);

            if (this.getBreaks() != null) {
                dto.setBreaks(this.getBreaks().stream()
                        .filter(Objects::nonNull)
                        .map(Break::toBarGraphDto)
                        .collect(Collectors.toList()));
            } else {
                dto.setBreaks(Collections.emptyList());
            }
        } catch (Exception e) {
            // ignore
        }
        return dto;
    }

    public LinegraphAttedance toLineChartDto() {
        LinegraphAttedance dto = new LinegraphAttedance();
        try {
            dto.setDate(String.valueOf(this.getDate().getDayOfMonth()));
            dto.setMonth(this.getMonth());
            dto.setYear(this.getYear());
            Duration effective = this.getEffectiveHours();
            Duration breakDur = this.breakDuration();
            dto.setWorking_hour(effective != null ? Helper.DurationToPercentage(effective) : 0.0);
            dto.setBreak_hour(breakDur != null ? Helper.DurationToPercentage(breakDur) : 0.0);
        } catch (Exception e) {
            dto.setWorking_hour(0.0);
            dto.setBreak_hour(0.0);
        }
        return dto;
    }

    public SimpleAttendanceDto toSimpleAttendanceDto() {
        SimpleAttendanceDto dto = new SimpleAttendanceDto();
        try {
            dto.setEmpId(this.getEmployeeId());
            dto.setDate(this.getDate() != null ? String.valueOf(this.getDate().getDayOfMonth()) : null);
            dto.setLoginTime(this.getLoginTime() != null ? Helper.localDateTimeToString(this.getLoginTime()) : null);
            dto.setLogoutTime(this.getLogoutTime() != null ? Helper.localDateTimeToString(this.getLogoutTime()) : null);
            dto.setAttendanceId(this.getAttendanceId()!=null?this.getAttendanceId():null);
            dto.setBreaks(this.getBreaks() != null ? this.getBreaks().stream().filter(Objects::nonNull).map(Break::toSimpleBreakDto).collect(Collectors.toList()) : null);
        } catch (Exception e) {
            // ignore
        }
        return dto;
    }
}

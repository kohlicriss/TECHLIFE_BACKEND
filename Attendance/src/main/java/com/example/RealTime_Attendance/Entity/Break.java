package com.example.RealTime_Attendance.Entity;

import com.example.RealTime_Attendance.Dto.Breaksdto;
import com.example.RealTime_Attendance.Dto.SimpleBreakDto;
import com.example.RealTime_Attendance.service.Helper;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.beans.SimpleBeanInfo;
import java.time.Duration;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name="breaks", schema="attendance")
@ToString(exclude = "attendance") 
public class Break {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne
    @JoinColumn(name="attendance")
    @JsonBackReference
    private Attendance attendance;

    public SimpleBreakDto toSimpleBreakDto() {
        SimpleBreakDto dto = new SimpleBreakDto();
        try {
            dto.setStartTime(this.getStartTime() != null ? Helper.localDateTimeToString(this.getStartTime()) : null);
            dto.setEndTime(this.getEndTime() != null ? Helper.localDateTimeToString(this.getEndTime()) : null);
        } catch (Exception e) {
            // ignore
        }
        return dto;
    }

    public Breaksdto toBarGraphDto() {
        Breaksdto dto = new Breaksdto();
        try {
            LocalDateTime start = this.getStartTime();
            LocalDateTime end = this.getEndTime() != null ? this.getEndTime() : LocalDateTime.now();

            if (start == null) {
                dto.setHour(0.0);
                dto.setTime(null);
                return dto;
            }

            Duration duration = Duration.between(start, end);
            double diffTime = (double) duration.toMinutes() / 60;
            double rounded = Math.round(diffTime * 10.0) / 10.0;

            String time = (start != null && end != null)
                    ? Helper.localDateTimeToString(start) + " - " + Helper.localDateTimeToString(end)
                    : null;

            dto.setHour(rounded);
            dto.setTime(time);

        } catch (Exception e) {
            System.err.println("Error converting to Breaksdto: " + e.getMessage());
            dto.setHour(0.0);
            dto.setTime(null);
        }

        return dto;
    }
}

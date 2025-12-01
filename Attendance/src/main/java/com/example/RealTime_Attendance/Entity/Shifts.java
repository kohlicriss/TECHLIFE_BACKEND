package com.example.RealTime_Attendance.Entity;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.example.RealTime_Attendance.Dto.ShiftsDto;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shifts", schema = "attendance")
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "shiftName")
public class Shifts {
    @Id
    private String shiftName;

    @Column(name = "start_time")
    private LocalTime startTime;
    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "half_time")
    private LocalTime halfTime;

    @Column
    private Duration acceptedBreakTime;

    @Column
    private Duration takeAttendanceAfter;

    @OneToMany(mappedBy = "shifts", cascade = CascadeType.ALL)
    private List<PersonalLeaves> personalLeaves;


    public ShiftsDto toShiftsDto(){
        ShiftsDto shiftsDto = new ShiftsDto();
        shiftsDto.setName(this.getShiftName());
        shiftsDto.setStart(this.getStartTime());
        shiftsDto.setEnd(this.getEndTime());
        List<String> employeeIds = new ArrayList<>();
        for(PersonalLeaves p: this.getPersonalLeaves()){
            employeeIds.add(p.getEmployeeId());
        }
        shiftsDto.setEmployeeIds(employeeIds);
        return shiftsDto;
    }
}


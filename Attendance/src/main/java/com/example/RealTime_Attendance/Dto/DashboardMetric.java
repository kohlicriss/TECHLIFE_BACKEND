package com.example.RealTime_Attendance.Dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardMetric{
    private String value;
    private String description;
    private String trend;
    private String trendPercentage;
    private String trendPeriod;
}

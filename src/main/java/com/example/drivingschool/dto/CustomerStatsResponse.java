package com.example.drivingschool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerStatsResponse {
    private long totalRegistered;
    private long currentMonthEnrolled;
    private long totalCompleted;
    private long liveSchedules; // Active schedules for today
    private List<Map<String, Object>> enrollmentTrend; // Last 3 months data
}
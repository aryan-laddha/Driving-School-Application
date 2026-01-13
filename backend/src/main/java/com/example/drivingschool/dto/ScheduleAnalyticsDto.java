package com.example.drivingschool.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleAnalyticsDto {
    // Summary Stats
    private long todayTotal;
    private long todayCompleted;

    // Graph Data
    private List<DailyScheduleStats> lastSevenDays;

    @Data
    @Builder
    public static class DailyScheduleStats {
        private String date; // e.g., "Mon 24"
        private long completed;
        private long incomplete; // Scheduled + Rescheduled
        private long cancelled;
    }
}
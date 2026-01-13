package com.example.drivingschool.service;

import com.example.drivingschool.dto.ScheduleAnalyticsDto;
import com.example.drivingschool.model.Schedule;
import com.example.drivingschool.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleAnalyticsService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    public ScheduleAnalyticsDto getScheduleStats() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE dd");

        // 1. Fetch Today's Header Stats
        List<Schedule> todaySchedules = scheduleRepository.findByDate(today);
        long todayTotal = todaySchedules.size();
        long todayCompleted = todaySchedules.stream()
                .filter(s -> s.getStatus() == Schedule.ScheduleStatus.COMPLETED)
                .count();

        // 2. Fetch Last 7 Days for Graph
        List<ScheduleAnalyticsDto.DailyScheduleStats> dailyStats = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<Schedule> daySchedules = scheduleRepository.findByDate(date);

            long completed = daySchedules.stream()
                    .filter(s -> s.getStatus() == Schedule.ScheduleStatus.COMPLETED)
                    .count();

            long incomplete = daySchedules.stream()
                    .filter(s -> s.getStatus() == Schedule.ScheduleStatus.SCHEDULED ||
                            s.getStatus() == Schedule.ScheduleStatus.RESCHEDULED)
                    .count();

            long cancelled = daySchedules.stream()
                    .filter(s -> s.getStatus() == Schedule.ScheduleStatus.CANCELLED)
                    .count();

            dailyStats.add(ScheduleAnalyticsDto.DailyScheduleStats.builder()
                    .date(date.format(formatter))
                    .completed(completed)
                    .incomplete(incomplete)
                    .cancelled(cancelled)
                    .build());
        }

        return ScheduleAnalyticsDto.builder()
                .todayTotal(todayTotal)
                .todayCompleted(todayCompleted)
                .lastSevenDays(dailyStats)
                .build();
    }
}
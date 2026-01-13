package com.example.drivingschool.controller;

import com.example.drivingschool.dto.ScheduleAnalyticsDto;
import com.example.drivingschool.service.ScheduleAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
public class ScheduleAnalyticsController {

    @Autowired
    private ScheduleAnalyticsService analyticsService;

    @GetMapping("/schedules")
    public ResponseEntity<ScheduleAnalyticsDto> getScheduleAnalytics() {
        return ResponseEntity.ok(analyticsService.getScheduleStats());
    }
}
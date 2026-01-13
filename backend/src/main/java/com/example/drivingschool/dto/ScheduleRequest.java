package com.example.drivingschool.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduleRequest {
    private Long courseId;
    private Long customerId;
    private Long instructorId;

    // Change this to String
    private String vehicleNumber;

    private LocalDate startDate;       // first day of course
    private LocalTime startTime;
}

package com.example.drivingschool.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleRequestDto {
    private Long scheduleId;          // ID of the cancelled schedule
    private LocalDate newDate;        // New date for rescheduling
    private LocalTime newStartTime;   // New start time
}

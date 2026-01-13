package com.example.drivingschool.dto;

import com.example.drivingschool.model.Schedule.ScheduleStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponseDto {
    private Long id;
    private Long instructorId;
    private String instructorName;
    private Long customerId;
    private String customerName;
    private String customerContact;
    private String customerAddress;
    private Long courseId;
    private String courseName;
    private LocalDate courseStartDate;
    private LocalDate courseEndDate;
    private String vehicleNumber;
    private String vehicleName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private ScheduleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean pickAndDrop;
}

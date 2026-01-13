package com.example.drivingschool.dto;

import com.example.drivingschool.model.Schedule;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSchdeuleDTO {
    private Long id;
    private Long instructorId;
    private String instructorName;

    private Long customerId;
    private String customerName;

    private Long courseId;
    private String courseName;

    private String vehicleNumber;
    private String vehicleName;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    private Schedule.ScheduleStatus status;

}

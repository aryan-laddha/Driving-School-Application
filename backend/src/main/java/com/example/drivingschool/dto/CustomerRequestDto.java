package com.example.drivingschool.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequestDto {
    private String name;
    private String contact;
    private String address;
    private Long courseId;             // Selected course
    private String vehicleNumber;      // Assigned vehicle
    private Long assignedInstructorId; // Assigned instructor
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime preferredStartTime;
    private LocalTime preferredEndTime;
    private boolean pickAndDrop;



}

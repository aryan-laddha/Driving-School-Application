package com.example.drivingschool.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDto {
    private Long id;
    private String name;
    private String contact;
    private Long courseId;
    private String courseName;
    private String vehicleNumber;
    private String vehicleName;
    private Long assignedInstructorId;
    private String assignedInstructorName;

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalTime preferredStartTime;
    private LocalTime preferredEndTime;
    private boolean pickAndDrop;
    private String address;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}

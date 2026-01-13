package com.example.drivingschool.dto;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerScheduleUpdateRequest {
    private Long customerId;

    // Optional fields: If null, the existing value is kept
    private Long newInstructorId;
    private String newVehicleNumber;
    private LocalTime newStartTime;
    private Boolean pickAndDrop;
}
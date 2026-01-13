package com.example.drivingschool.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableSlotsRequestDto {
    private Long instructorId;
    private String vehicleNumber;
    private LocalDate date;
    private int slotDurationHours;
}

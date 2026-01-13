package com.example.drivingschool.dto;

import com.example.drivingschool.model.VehicleType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRequestDto {
    private Long courseId;                     // For update; can be null during create
    private String courseName;                 // Course name
    private String description;          // Optional description
    private VehicleType vehicleType;     // Enum
    private String vehicleSubCategory;   // e.g., "Manual", "Automatic"
    private BigDecimal price;                // Matches entity
    private int totalDays;               // totalDays in entity
    private int durationPerDayHours;     // Hours per day
    private boolean active = true;       // Default true
}

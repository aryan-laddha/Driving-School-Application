package com.example.drivingschool.dto;

import com.example.drivingschool.model.VehicleType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponseDto {
    private Long courseId;
    private String courseName;
    private String description;
    private VehicleType vehicleType;
    private String vehicleSubCategory;
    private BigDecimal price;
    private int totalDays;
    private int durationPerDayHours;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

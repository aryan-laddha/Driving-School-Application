package com.example.drivingschool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DistanceResponseDto {
    private boolean success;
    private double distanceKm;
    private String message;
}
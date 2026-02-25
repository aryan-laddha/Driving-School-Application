package com.example.drivingschool.dto;

import com.example.drivingschool.model.Vehicle;
import com.example.drivingschool.model.VehicleType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponseDto {

    private String vehicleNumber;
    private String vehicleName;
    private VehicleType vehicleType;
    private boolean active;
    private String createdAt;
    private String updatedAt;
}

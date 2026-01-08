package com.example.drivingschool.dto;

import com.example.drivingschool.model.VehicleType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequestDto {
    private String vehicleNumber;   // PK, used for add/update
    private VehicleType vehicleType;
    private String vehicleName;
    private boolean active;
}

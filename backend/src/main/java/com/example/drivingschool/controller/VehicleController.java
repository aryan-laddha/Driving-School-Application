package com.example.drivingschool.controller;

import com.example.drivingschool.dto.ApiResponse;
import com.example.drivingschool.dto.VehicleRequestDto;
import com.example.drivingschool.dto.VehicleResponseDto;
import com.example.drivingschool.service.VehicleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VehicleResponseDto> addVehicle(@RequestBody VehicleRequestDto request) {
        VehicleResponseDto vehicle = vehicleService.addVehicle(request);
        return new ApiResponse<>(true, "Vehicle added successfully", vehicle);
    }

    @PutMapping("/{vehicleNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VehicleResponseDto> updateVehicle(@PathVariable String vehicleNumber,
                                                         @RequestBody VehicleRequestDto request) {
        VehicleResponseDto vehicle = vehicleService.updateVehicle(vehicleNumber, request);
        return new ApiResponse<>(true, "Vehicle updated successfully", vehicle);
    }

    @DeleteMapping("/soft-delete/{vehicleNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> softDelete(@PathVariable String vehicleNumber) {
        String message = vehicleService.softDeleteVehicle(vehicleNumber);
        return new ApiResponse<>(true, message, null);
    }

    @PostMapping("/restore/{vehicleNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> restoreVehicle(@PathVariable String vehicleNumber) {
        String message = vehicleService.restoreVehicle(vehicleNumber);
        return new ApiResponse<>(true, message, null);
    }

    @DeleteMapping("/hard-delete/{vehicleNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> hardDelete(@PathVariable String vehicleNumber) {
        String message = vehicleService.hardDeleteVehicle(vehicleNumber);
        return new ApiResponse<>(true, message, null);
    }

    @GetMapping
    public ApiResponse<List<VehicleResponseDto>> getAllVehicles() {
        List<VehicleResponseDto> vehicles = vehicleService.getAllVehicles();
        return new ApiResponse<>(true, "All vehicles fetched", vehicles);
    }
}

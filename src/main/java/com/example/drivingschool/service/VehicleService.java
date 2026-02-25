package com.example.drivingschool.service;

import com.example.drivingschool.dto.VehicleRequestDto;
import com.example.drivingschool.dto.VehicleResponseDto;
import com.example.drivingschool.exception.ResourceNotFoundException;
import com.example.drivingschool.model.Vehicle;
import com.example.drivingschool.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public VehicleResponseDto addVehicle(VehicleRequestDto request) {
        if (vehicleRepository.existsById(request.getVehicleNumber())) {
            throw new RuntimeException("Vehicle number already exists");
        }

        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(request.getVehicleNumber())
                .vehicleType(request.getVehicleType())
                .vehicleName(request.getVehicleName())
                .active(request.isActive())
                .build();

        vehicleRepository.save(vehicle); // flush to populate timestamps

        return mapToDto(vehicle);
    }

    @Transactional
    public VehicleResponseDto updateVehicle(String vehicleNumber, VehicleRequestDto request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        // Vehicle number cannot be updated
        if (request.getVehicleNumber() != null && !request.getVehicleNumber().equals(vehicleNumber)) {
            throw new RuntimeException("Vehicle number cannot be updated");
        }

        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setVehicleName(request.getVehicleName());
        vehicle.setActive(request.isActive());

        vehicleRepository.saveAndFlush(vehicle); // flush to update timestamps
        return mapToDto(vehicle);
    }

    @Transactional
    public String softDeleteVehicle(String vehicleNumber) {
        Vehicle vehicle = vehicleRepository.findById(vehicleNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with number: " + vehicleNumber));

        vehicle.setActive(false);
        vehicleRepository.saveAndFlush(vehicle); // flush to update timestamps
        return "Vehicle soft deleted successfully";
    }

    @Transactional
    public String restoreVehicle(String vehicleNumber) {
        Vehicle vehicle = vehicleRepository.findById(vehicleNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with number: " + vehicleNumber));

        vehicle.setActive(true);
        vehicleRepository.saveAndFlush(vehicle); // flush to update timestamps
        return "Vehicle restored successfully";
    }

    @Transactional
    public String hardDeleteVehicle(String vehicleNumber) {
        Vehicle vehicle = vehicleRepository.findById(vehicleNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        vehicleRepository.delete(vehicle);
        return "Vehicle permanently deleted successfully";
    }

    public List<VehicleResponseDto> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Mapping to DTO with timestamps
    private VehicleResponseDto mapToDto(Vehicle vehicle) {
        return VehicleResponseDto.builder()
                .vehicleNumber(vehicle.getVehicleNumber())
                .vehicleName(vehicle.getVehicleName())
                .vehicleType(vehicle.getVehicleType())
                .active(vehicle.isActive())
                .createdAt(vehicle.getCreatedAt() != null ? vehicle.getCreatedAt().toString() : null)
                .updatedAt(vehicle.getUpdatedAt() != null ? vehicle.getUpdatedAt().toString() : null)
                .build();
    }
}

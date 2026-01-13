package com.example.drivingschool.repository;

import com.example.drivingschool.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    long countByActiveTrue();

}

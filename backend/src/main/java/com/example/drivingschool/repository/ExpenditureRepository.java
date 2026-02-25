package com.example.drivingschool.repository;

import com.example.drivingschool.model.Expenditure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenditureRepository extends JpaRepository<Expenditure, Long> {
    List<Expenditure> findByVehicleVehicleNumber(String vehicleNumber);
}
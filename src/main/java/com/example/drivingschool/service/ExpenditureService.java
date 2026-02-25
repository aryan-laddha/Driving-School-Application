package com.example.drivingschool.service;

import com.example.drivingschool.dto.ExpenditureRequest;
import com.example.drivingschool.dto.ExpenditureResponse;
import com.example.drivingschool.model.Expenditure;
import com.example.drivingschool.model.Vehicle;
import com.example.drivingschool.repository.ExpenditureRepository;
import com.example.drivingschool.repository.UserRepository;
import com.example.drivingschool.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.drivingschool.model.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenditureService {

    @Autowired
    private ExpenditureRepository expenditureRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private UserRepository userRepository;

    public ExpenditureResponse addExpense(ExpenditureRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleNumber())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        Expenditure expenditure = Expenditure.builder()
                .expenseName(request.getExpenseName())
                .price(request.getPrice())
                .expenseType(request.getExpenseType())
                .expenseDate(request.getExpenseDate())
                .details(request.getDetails())
                .vehicle(vehicle)
                .user(request.getUser())
                .build();

        return mapToResponse(expenditureRepository.save(expenditure));
    }



    // Helper method to map Entity -> DTO
    private ExpenditureResponse mapToResponse(Expenditure exp) {
        String displayName = userRepository.findByUsername(exp.getUser())
                .map(User::getName)
                .orElse(exp.getUser());

        return ExpenditureResponse.builder()
                .id(exp.getId())
                .expenseName(exp.getExpenseName())
                .price(exp.getPrice())
                .expenseType(exp.getExpenseType())
                .expenseDate(exp.getExpenseDate())
                .details(exp.getDetails())
                .vehicleNumber(exp.getVehicle().getVehicleNumber())
                .vehicleName(exp.getVehicle().getVehicleName())
                .user(displayName) // 3. Now sending the "Name" instead of "Username"
                .build();
    }
    public List<ExpenditureResponse> getAllExpenses() {
        return expenditureRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<Expenditure> getExpensesByVehicle(String vehicleNumber) {
        return expenditureRepository.findByVehicleVehicleNumber(vehicleNumber);
    }

    public List<ExpenditureResponse> getByuser(String username) {
        return expenditureRepository.findByUser(username).stream()
                .map(this::mapToResponse) // This converts Entities to clean DTOs
                .collect(Collectors.toList());
    }
}
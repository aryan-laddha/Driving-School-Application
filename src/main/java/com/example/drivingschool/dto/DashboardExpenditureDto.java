package com.example.drivingschool.dto;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardExpenditureDto {
    private Double totalExpenditure;
    private Double fuelExpenses;
    private Double maintenanceExpenses;
    private Double otherExpenses;
    private Map<String, Double> expensesByVehicle; // Useful for a Bar Chart
}
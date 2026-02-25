package com.example.drivingschool.dto;

import com.example.drivingschool.model.ExpenseType;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenditureResponse {
    private Long id;
    private String expenseName;
    private Double price;
    private ExpenseType expenseType;
    private LocalDate expenseDate;
    private String details;
    private String user;
    private String vehicleNumber; // Flattened: just the ID string, not the whole object
    private String vehicleName;   // Optional: helpful for the UI table
}
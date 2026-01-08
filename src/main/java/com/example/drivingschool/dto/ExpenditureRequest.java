package com.example.drivingschool.dto;

import com.example.drivingschool.model.ExpenseType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ExpenditureRequest {
    private String expenseName;
    private Double price;
    private ExpenseType expenseType;
    private LocalDate expenseDate;
    private String details;
    private String vehicleNumber; // Link via ID string
}
package com.example.drivingschool.controller;

import com.example.drivingschool.dto.ApiResponse;
import com.example.drivingschool.dto.ExpenditureRequest;
import com.example.drivingschool.dto.ExpenditureResponse;
import com.example.drivingschool.model.Expenditure;
import com.example.drivingschool.service.ExpenditureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenditures")
public class ExpenditureController {

    @Autowired
    private ExpenditureService expenditureService;

//    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ApiResponse<ExpenditureResponse>> createExpense(@RequestBody ExpenditureRequest request) {
//        try {
//            ExpenditureResponse saved = expenditureService.addExpense(request);
//            return ResponseEntity.ok(new ApiResponse<>(true, "Expense recorded", saved));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
//        }
//    }
//
//    @GetMapping
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ApiResponse<List<ExpenditureResponse>>> getAll() {
//        List<ExpenditureResponse> list = expenditureService.getAllExpenses();
//        return ResponseEntity.ok(new ApiResponse<>(true, "Data retrieved", list));
//    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExpenditureResponse>> createExpense(@RequestBody ExpenditureRequest request) {
        try {
            ExpenditureResponse saved = expenditureService.addExpense(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Expense recorded", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ExpenditureResponse>>> getAll() {
        List<ExpenditureResponse> list = expenditureService.getAllExpenses();
        return ResponseEntity.ok(new ApiResponse<>(true, "Data retrieved", list));
    }

    @GetMapping("/vehicle/{vehicleNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Expenditure>>> getByVehicle(@PathVariable String vehicleNumber) {
        List<Expenditure> list = expenditureService.getExpensesByVehicle(vehicleNumber);
        return ResponseEntity.ok(new ApiResponse<>(true, "Expenses for " + vehicleNumber, list));
    }
}
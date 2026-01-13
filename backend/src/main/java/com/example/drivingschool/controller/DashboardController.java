package com.example.drivingschool.controller;

import com.example.drivingschool.dto.CustomerStatsResponse;
import com.example.drivingschool.dto.DashboardExpenditureDto;
import com.example.drivingschool.dto.DashboardFinanceDto;
import com.example.drivingschool.service.DashboardService;
import com.example.drivingschool.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/finance-stats")
    public ResponseEntity<ApiResponse<DashboardFinanceDto>> getFinanceStats() {
        try {
            DashboardFinanceDto stats = dashboardService.getFinancialStats();
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Financial statistics retrieved successfully",
                    stats
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                    false,
                    "Error fetching dashboard data: " + e.getMessage(),
                    null
            ));
        }
    }

    @GetMapping("/customer-stats")
    public ResponseEntity<ApiResponse<CustomerStatsResponse>> getCustomerStats() {
        try {
            CustomerStatsResponse stats = dashboardService.getCustomerStats();
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Customer analytics retrieved successfully",
                    stats
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                    false,
                    "Error fetching customer dashboard data: " + e.getMessage(),
                    null
            ));
        }
    }

    @GetMapping("/expenditure-stats")
    public ResponseEntity<ApiResponse<DashboardExpenditureDto>> getExpenditureStats() {
        try {
            DashboardExpenditureDto stats = dashboardService.getExpenditureStats();
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Expenditure statistics retrieved successfully",
                    stats
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                    false,
                    "Error fetching expenditure dashboard data: " + e.getMessage(),
                    null
            ));
        }
    }
}
package com.example.drivingschool.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFinanceDto {
    private List<MonthlyRevenue> lastThreeMonths;

    // For the Bullet/Stack Graph (Total Overview)
    private BigDecimal totalPaidAmount;    // Green Part
    private BigDecimal totalPendingAmount; // Red Part
    private BigDecimal thisMonthIncome;    // Total collected this month
}

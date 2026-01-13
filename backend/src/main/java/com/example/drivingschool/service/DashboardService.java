package com.example.drivingschool.service;

import com.example.drivingschool.dto.CustomerStatsResponse;
import com.example.drivingschool.dto.DashboardExpenditureDto;
import com.example.drivingschool.dto.DashboardFinanceDto;
import com.example.drivingschool.dto.MonthlyRevenue;
import com.example.drivingschool.model.Expenditure;
import com.example.drivingschool.model.ExpenseType;
import com.example.drivingschool.repository.CustomerRepository;
import com.example.drivingschool.repository.ExpenditureRepository;
import com.example.drivingschool.repository.PaymentRepository;
import com.example.drivingschool.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ExpenditureRepository expenditureRepository;



    public DashboardFinanceDto getFinancialStats() {
        // 1. Calculate Monthly Revenue for Bar Chart
        List<MonthlyRevenue> monthlyData = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            // Calculate the first day of the target month
            LocalDateTime start = LocalDate.now().minusMonths(i).withDayOfMonth(1).atStartOfDay();
            // Calculate the last second of that same month
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);

            // Use the new BETWEEN query
            BigDecimal monthIncome = paymentRepository.sumIncomeBetween(start, end);

            monthlyData.add(new MonthlyRevenue(
                    start.getMonth().name(),
                    monthIncome != null ? monthIncome : BigDecimal.ZERO
            ));
        }

        // 2. Get Data for Bullet/Stack Graph
        Object[] stackStats = (Object[]) paymentRepository.getGlobalPaidVsPending().get(0);
        BigDecimal totalPaid = (BigDecimal) stackStats[0];
        BigDecimal totalPending = (BigDecimal) stackStats[1];

        // 3. Current Month Income
        LocalDateTime firstDayOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        BigDecimal thisMonthIncome = paymentRepository.sumIncomeSince(firstDayOfMonth);

        return DashboardFinanceDto.builder()
                .lastThreeMonths(monthlyData)
                .totalPaidAmount(totalPaid != null ? totalPaid : BigDecimal.ZERO)
                .totalPendingAmount(totalPending != null ? totalPending : BigDecimal.ZERO)
                .thisMonthIncome(thisMonthIncome != null ? thisMonthIncome : BigDecimal.ZERO)
                .build();
    }


    public CustomerStatsResponse getCustomerStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);

        // Calculate 3-month trend
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            LocalDateTime monthStart = firstDayOfMonth.minusMonths(i);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            long count = customerRepository.countByCreatedAtBetween(monthStart, monthEnd);

            trend.add(Map.of(
                    "month", monthStart.getMonth().toString().substring(0, 3),
                    "count", count
            ));
        }

        return CustomerStatsResponse.builder()
                .totalRegistered(customerRepository.countByDeletedFalse())
                .currentMonthEnrolled(customerRepository.countByCreatedAtAfter(firstDayOfMonth))
                .totalCompleted(customerRepository.countByCompletedTrue())
                .liveSchedules(scheduleRepository.countLiveSchedules())
                .enrollmentTrend(trend)
                .build();
    }

// Inside DashboardService.java

    public DashboardExpenditureDto getExpenditureStats() {
        // Fetch all expenditures (you can also limit this to the current month)
        List<Expenditure> allExpenses = expenditureRepository.findAll();

        Double total = allExpenses.stream().mapToDouble(Expenditure::getPrice).sum();

        Double fuel = allExpenses.stream()
                .filter(e -> e.getExpenseType() == ExpenseType.FUEL)
                .mapToDouble(Expenditure::getPrice).sum();

        Double maintenance = allExpenses.stream()
                .filter(e -> e.getExpenseType() == ExpenseType.MAINTENANCE)
                .mapToDouble(Expenditure::getPrice).sum();

        Double others = allExpenses.stream()
                .filter(e -> e.getExpenseType() == ExpenseType.OTHER)
                .mapToDouble(Expenditure::getPrice).sum();

        // Grouping by vehicle number for a chart
        Map<String, Double> byVehicle = allExpenses.stream()
                .collect(Collectors.groupingBy(
                        exp -> exp.getVehicle() != null ? exp.getVehicle().getVehicleNumber() : "Unknown",
                        Collectors.summingDouble(Expenditure::getPrice)
                ));

        return DashboardExpenditureDto.builder()
                .totalExpenditure(total)
                .fuelExpenses(fuel)
                .maintenanceExpenses(maintenance)
                .otherExpenses(others)
                .expensesByVehicle(byVehicle)
                .build();
    }
}
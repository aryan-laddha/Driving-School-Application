package com.example.drivingschool.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyRevenue {
    private String month;
    private BigDecimal revenue;
}
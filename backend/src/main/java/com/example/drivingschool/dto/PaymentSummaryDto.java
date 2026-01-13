package com.example.drivingschool.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSummaryDto {

    private Long customerId;
    private String customerName;
    private BigDecimal totalCoursePrice;
    private BigDecimal amountPaid;
    private BigDecimal remainingAmount;
}
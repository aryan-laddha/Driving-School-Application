package com.example.drivingschool.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAdjustmentRequestDto {
    private BigDecimal newBasePrice;
    private BigDecimal newExtraCharges;
    private BigDecimal newDiscount;
}
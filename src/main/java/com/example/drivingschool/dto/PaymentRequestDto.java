package com.example.drivingschool.dto;

import com.example.drivingschool.model.PaymentType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    private Long customerId;

    private BigDecimal extraCharges;

    private BigDecimal discount;

    private BigDecimal initialPayment;

    private PaymentType paymentType;

    private Boolean pickAndDrop; // optional if needed

    private LocalDate lastPaymentDate;
}

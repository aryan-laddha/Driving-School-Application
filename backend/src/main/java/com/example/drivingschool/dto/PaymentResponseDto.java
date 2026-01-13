package com.example.drivingschool.dto;

import com.example.drivingschool.model.PaymentType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {

    private Long id;

    private Long customerId;
    private String customerName;
    private String customerContact;

    private BigDecimal basePrice;
    private BigDecimal extraCharges;
    private BigDecimal discount;

    private BigDecimal totalPrice;
    private BigDecimal initialPayment;

    private boolean paymentCompleted;

    private PaymentType paymentType;

    private LocalDate lastPaymentDate;

    private LocalDate courseStartDate;
    private LocalDate courseEndDate;

    private LocalDateTime createdAt;
}

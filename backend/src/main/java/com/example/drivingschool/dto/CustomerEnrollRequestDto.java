package com.example.drivingschool.dto;

import com.example.drivingschool.model.PaymentType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEnrollRequestDto {
    private Long customerId;              // add this
    private String name;
    private String contact;
    private String address;
    private Long courseId;
    private String vehicleNumber;
    private Long instructorId;
    private LocalDate startDate;
    private LocalTime preferredStartTime;
    private boolean pickAndDrop;
    private BigDecimal extraCharges;      // optional
    private BigDecimal discount;          // optional
    private BigDecimal initialPayment;    // initial amount paid
    private PaymentType paymentType;      // CASH, UPI, CARD
}

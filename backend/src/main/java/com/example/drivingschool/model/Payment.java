package com.example.drivingschool.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Price from course at the moment of purchase
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    // Extra charges (optional)
    @Column(precision = 10, scale = 2)
    private BigDecimal extraCharges;

    // Total price = basePrice + extraCharges + pick/drop(if any)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // Initial amount paid
    @Column(precision = 10, scale = 2)
    private BigDecimal initialPayment;

    // Discount amount applied
    @Column(precision = 10, scale = 2)
    private BigDecimal discount;


    // Last payment date (final payment timestamp)
    private LocalDate lastPaymentDate;

    // Payment completed?
    @Column(nullable = false)
    private boolean paymentCompleted = false;

    // Payment mode
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    // Customer who made this payment
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // These are copied from Customer for accounting integrity
    private LocalDate courseStartDate;

    private LocalDate courseEndDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

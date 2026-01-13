package com.example.drivingschool.repository;

import com.example.drivingschool.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByCustomerId(Long customerId);

    List<Payment> findByPaymentCompletedFalse();

    // Filter revenue strictly within a specific month range
    @Query("SELECT SUM(p.initialPayment) FROM Payment p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate")
    BigDecimal sumIncomeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Sum for the Stack Graph: Paid vs Pending
    @Query("SELECT " +
            "SUM(p.initialPayment), " +
            "SUM(p.totalPrice - p.initialPayment - COALESCE(p.discount, 0)) " +
            "FROM Payment p")
    List<Object[]> getGlobalPaidVsPending();

    @Query("SELECT SUM(p.initialPayment) FROM Payment p WHERE p.createdAt >= :startDate")
    BigDecimal sumIncomeSince(@Param("startDate") LocalDateTime startDate);
}

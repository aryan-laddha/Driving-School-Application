package com.example.drivingschool.repository;

import com.example.drivingschool.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByDeletedFalse();


    long countByCreatedAtAfter(LocalDateTime date);
    long countByCompletedTrue();
    long countByDeletedFalse();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate")
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

}

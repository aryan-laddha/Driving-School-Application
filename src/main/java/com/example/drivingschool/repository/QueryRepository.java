package com.example.drivingschool.repository;


import com.example.drivingschool.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryRepository extends JpaRepository<Query, Long> {
    // Custom finder methods can be added here, e.g.,
    // List<Query> findByIsResolved(boolean isResolved);
}
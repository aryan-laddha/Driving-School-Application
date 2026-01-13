package com.example.drivingschool.repository;

import com.example.drivingschool.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find a user by username who is not soft deleted
    Optional<User> findByUsernameAndDeletedFalse(String username);

    // Find by username regardless of deleted status
    Optional<User> findByUsername(String username);

    // Check if username exists (for registration)
    boolean existsByUsername(String username);

    long countByAccessTrueAndDeletedFalse();

    /**
     * Counts users waiting for Admin Approval.
     * Logic: access = false AND deleted = false
     */
    long countByAccessFalseAndDeletedFalse();



}

package com.example.drivingschool.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    @Column(nullable = false)
    private String courseName; // e.g. "4 Wheeler Automatic Driving Course"

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    // Vehicle info related to this course
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType; // e.g. FOUR_WHEELER

    @Column(nullable = false)
    private String vehicleSubCategory; // e.g. "Automatic", "Manual", "EV", etc.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // e.g. 3500.00

    @Column(nullable = false)
    private int durationPerDayHours; // e.g. 2 (hours per day)

    @Column(nullable = false)
    private int totalDays; // e.g. 10 (total course days)

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}


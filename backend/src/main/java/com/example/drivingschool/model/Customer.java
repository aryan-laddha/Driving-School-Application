package com.example.drivingschool.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalTime;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String contact;

    @Column
    private String address;

    // Selected course
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // Assigned vehicle
    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    // Assigned instructor (user)
    @ManyToOne
    @JoinColumn(name = "instructor_id", nullable = false)
    private User assignedInstructor;

    @Column
    private LocalTime preferredStartTime;

    @Column
    private LocalTime preferredEndTime;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;


    private boolean pickAndDrop = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private Boolean deleted = false;

    @Column
    private Boolean completed = false;

}

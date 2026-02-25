package com.example.drivingschool.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which instructor or admin handles this schedule
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User instructor;

    // Which customer is being trained
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Which course the session belongs to
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // Which vehicle is used
    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    // Schedule date (for daily tracking)
    @Column(nullable = false)
    private LocalDate date;

    // Time slots
    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20) // <--- FIX: Explicitly set column length for safety
    private ScheduleStatus status = ScheduleStatus.SCHEDULED;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalTime resourceStartTime;
    private LocalTime resourceEndTime;

    public enum ScheduleStatus {
        SCHEDULED,
        COMPLETED,
        RESCHEDULED,
        CANCELLED
    }
}
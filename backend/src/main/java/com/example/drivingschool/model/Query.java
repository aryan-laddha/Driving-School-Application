package com.example.drivingschool.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "queries")
public class Query {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    private String phoneNumber;

    // Store the date/time the query was submitted
    private LocalDateTime submissionDate = LocalDateTime.now();

    private String queryText;

    // Flags for tracking
    private boolean isFollowUpRequired = true; // Default: Yes
    private boolean isResolved = false;      // Default: No

    // Getters and Setters (omitted for brevity)


}
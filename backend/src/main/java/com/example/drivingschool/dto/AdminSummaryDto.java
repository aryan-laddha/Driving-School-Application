package com.example.drivingschool.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSummaryDto {
    private long totalActiveUsers;
    private long pendingApprovalUsers;

    // Course & Vehicle Stats
    private long activeCourses;
    private long activeVehicles;
}

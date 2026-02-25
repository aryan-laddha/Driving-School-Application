package com.example.drivingschool.service;

import com.example.drivingschool.dto.AdminSummaryDto;
import com.example.drivingschool.repository.CourseRepository;
import com.example.drivingschool.repository.UserRepository;
import com.example.drivingschool.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SummaryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    public AdminSummaryDto getAdminSummary() {
        return AdminSummaryDto.builder()
                // Active users are those with access=true and not deleted
                .totalActiveUsers(userRepository.countByAccessTrueAndDeletedFalse())
                // Pending users are those with access=false and not deleted
                .pendingApprovalUsers(userRepository.countByAccessFalseAndDeletedFalse())
                // Counts for Courses and Vehicles
                .activeCourses(courseRepository.countByActiveTrue())
                .activeVehicles(vehicleRepository.countByActiveTrue())
                .build();
    }
}
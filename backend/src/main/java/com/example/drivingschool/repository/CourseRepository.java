package com.example.drivingschool.repository;

import com.example.drivingschool.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // Find all active courses
    List<Course> findByActiveTrue();

        long countByActiveTrue();


}

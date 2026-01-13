package com.example.drivingschool.controller;

import com.example.drivingschool.dto.ApiResponse;
import com.example.drivingschool.dto.CourseRequestDto;
import com.example.drivingschool.dto.CourseResponseDto;
import com.example.drivingschool.service.CourseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CourseResponseDto> addCourse(@RequestBody CourseRequestDto request) {
        CourseResponseDto course = courseService.addCourse(request);
        return new ApiResponse<>(true, "Course added successfully", course);
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CourseResponseDto> updateCourse(@PathVariable Long courseId,
                                                       @RequestBody CourseRequestDto request) {
        CourseResponseDto course = courseService.updateCourse(courseId, request);
        return new ApiResponse<>(true, "Course updated successfully", course);
    }

    @DeleteMapping("/soft-delete/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> softDelete(@PathVariable Long courseId) {
        String message = courseService.softDeleteCourse(courseId);
        return new ApiResponse<>(true, message, null);
    }

    @PostMapping("/restore/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> restoreCourse(@PathVariable Long courseId) {
        String message = courseService.restoreCourse(courseId);
        return new ApiResponse<>(true, message, null);
    }

    @DeleteMapping("/hard-delete/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> hardDelete(@PathVariable Long courseId) {
        String message = courseService.hardDeleteCourse(courseId);
        return new ApiResponse<>(true, message, null);
    }

    @GetMapping
    public ApiResponse<List<CourseResponseDto>> getAllCourses() {
        List<CourseResponseDto> courses = courseService.getAllCourses();
        return new ApiResponse<>(true, "All courses fetched", courses);
    }

    @GetMapping("/{courseId}")
    public ApiResponse<CourseResponseDto> getCourseById(@PathVariable Long courseId) {
        CourseResponseDto course = courseService.getCourseById(courseId);
        return new ApiResponse<>(true, "Course fetched successfully", course);
    }

}

    package com.example.drivingschool.service;

    import com.example.drivingschool.dto.CourseRequestDto;
    import com.example.drivingschool.dto.CourseResponseDto;
    import com.example.drivingschool.exception.ResourceNotFoundException;
    import com.example.drivingschool.model.Course;
    import com.example.drivingschool.repository.CourseRepository;
    import org.springframework.stereotype.Service;

    import java.util.List;
    import java.util.stream.Collectors;
    @Service
    public class CourseService {

        private final CourseRepository courseRepository;

        public CourseService(CourseRepository courseRepository) {
            this.courseRepository = courseRepository;
        }

        // Add a new course
        public CourseResponseDto addCourse(CourseRequestDto request) {
            // No need to check ID, DB will generate it automatically

            Course course = Course.builder()
                    .courseName(request.getCourseName())
                    .description(request.getDescription())
                    .vehicleType(request.getVehicleType())
                    .vehicleSubCategory(request.getVehicleSubCategory())
                    .price(request.getPrice())
                    .totalDays(request.getTotalDays())
                    .durationPerDayHours(request.getDurationPerDayHours())
                    .active(request.isActive())
                    .build();

            courseRepository.save(course);
            return mapToDto(course);
        }

        // Update existing course
        public CourseResponseDto updateCourse(Long courseId, CourseRequestDto request) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

            // update fields
            course.setCourseName(request.getCourseName());
            course.setDescription(request.getDescription());
            course.setVehicleType(request.getVehicleType());
            course.setVehicleSubCategory(request.getVehicleSubCategory());
            course.setPrice(request.getPrice());
            course.setTotalDays(request.getTotalDays());
            course.setDurationPerDayHours(request.getDurationPerDayHours());
            course.setActive(request.isActive());

            courseRepository.save(course);
            return mapToDto(course);
        }

        // Soft delete
        public String softDeleteCourse(Long courseId) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

            course.setActive(false);
            courseRepository.save(course);
            return "Course soft deleted successfully";
        }

        // Restore
        public String restoreCourse(Long courseId) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

            course.setActive(true);
            courseRepository.save(course);
            return "Course restored successfully";
        }

        // Hard delete
        public String hardDeleteCourse(Long courseId) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

            courseRepository.delete(course);
            return "Course permanently deleted successfully";
        }

        // Get all courses
        public List<CourseResponseDto> getAllCourses() {
            return courseRepository.findAll()
                    .stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        // Get course by ID
        public CourseResponseDto getCourseById(Long courseId) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
            return mapToDto(course);
        }

        // Mapper
        private CourseResponseDto mapToDto(Course course) {
            return CourseResponseDto.builder()
                    .courseId(course.getCourseId())
                    .courseName(course.getCourseName())
                    .description(course.getDescription())
                    .vehicleType(course.getVehicleType())
                    .vehicleSubCategory(course.getVehicleSubCategory())
                    .price(course.getPrice())
                    .totalDays(course.getTotalDays())
                    .durationPerDayHours(course.getDurationPerDayHours())
                    .active(course.isActive())
                    .createdAt(course.getCreatedAt())
                    .updatedAt(course.getUpdatedAt())
                    .build();
        }
    }

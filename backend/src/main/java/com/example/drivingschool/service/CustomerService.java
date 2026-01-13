package com.example.drivingschool.service;

import com.example.drivingschool.dto.*;
import com.example.drivingschool.model.*;
import com.example.drivingschool.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerService {

    @Autowired
    private WhatsAppService whatsappService;

    private final CustomerRepository customerRepository;
    private final CourseRepository courseRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final PaymentRepository paymentRepository;

    public CustomerService(CustomerRepository customerRepository,
                           CourseRepository courseRepository,
                           VehicleRepository vehicleRepository,
                           UserRepository userRepository,
                           ScheduleRepository scheduleRepository,
                           PaymentRepository paymentRepository) {
        this.customerRepository = customerRepository;
        this.courseRepository = courseRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.paymentRepository = paymentRepository;
    }

    // ---------------- Basic CRUD methods ----------------

    public CustomerResponseDto addCustomer(CustomerRequestDto request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleNumber())
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));
        User instructor = userRepository.findById(request.getAssignedInstructorId())
                .orElseThrow(() -> new EntityNotFoundException("Instructor not found"));

        Customer customer = Customer.builder()
                .name(request.getName())
                .contact(request.getContact())
                .address(request.getAddress()) // <-- added address mapping
                .course(course)
                .vehicle(vehicle)
                .assignedInstructor(instructor)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .preferredStartTime(request.getPreferredStartTime())
                .preferredEndTime(request.getPreferredEndTime())
                .active(true)
                .deleted(false)
                .build();
        Customer saved = customerRepository.save(customer);
        return mapToDto(saved);
    }

    public CustomerResponseDto updateCustomer(Long id, CustomerRequestDto request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleNumber())
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));
        User instructor = userRepository.findById(request.getAssignedInstructorId())
                .orElseThrow(() -> new EntityNotFoundException("Instructor not found"));

        customer.setName(request.getName());
        customer.setContact(request.getContact());
        customer.setCourse(course);
        customer.setVehicle(vehicle);
        customer.setAssignedInstructor(instructor);
        // Note: You can update time/date fields here if needed
        // customer.setStartDate(request.getStartDate());
        // customer.setEndDate(request.getEndDate());
        // customer.setPreferredStartTime(request.getPreferredStartTime());
        // customer.setPreferredEndTime(request.getPreferredEndTime());

        Customer updated = customerRepository.save(customer);
        return mapToDto(updated);
    }

    public List<CustomerResponseDto> getAllCustomers() {
        // Business Logic: Automatically mark customers inactive if their course end date has passed.
        checkAndDeactivateExpiredCustomers();

        return customerRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public CustomerResponseDto getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        return mapToDto(customer);
    }

    /**
     * Toggles customer's active status.
     * If deactivating (soft deleting), deletes all associated schedules.
     */
    public void softDeleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        boolean currentlyActive = customer.isActive();

        // Toggle active status
        customer.setActive(!currentlyActive);

        // Business Logic: If the customer is being set to INACTIVE (was active),
        // it means they dropped out, so delete all their schedules.
        if (currentlyActive) {
            scheduleRepository.deleteByCustomer(customer);
        }

        customerRepository.save(customer);
    }

    /**
     * Permanently deletes a customer and all associated schedules.
     */
    public void hardDeleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        // Business Logic: Delete all schedules first to avoid foreign key constraints
        // and ensure data is cleaned up.
        scheduleRepository.deleteByCustomer(customer);

        customerRepository.delete(customer);
    }

    // ---------------- Enrollment + Schedule creation ----------------

    public CustomerResponseDto enrollCustomer(CustomerEnrollRequestDto request) {

        // --- Fetch master entities ---
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleNumber())
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));

        User instructor = userRepository.findById(request.getInstructorId())
                .orElseThrow(() -> new EntityNotFoundException("Instructor not found"));

        boolean isPickAndDrop = request.isPickAndDrop();

        // =====================================================
        // 1️⃣ LESSON TIME (DO NOT MODIFY)
        // =====================================================
        LocalTime lessonStartTime = request.getPreferredStartTime();
        LocalTime lessonEndTime = lessonStartTime.plusHours(course.getDurationPerDayHours());

        // =====================================================
        // 2️⃣ RESOURCE TIME (INSTRUCTOR + VEHICLE BUSY WINDOW)
        // =====================================================
        LocalTime resourceStartTime = isPickAndDrop
                ? lessonStartTime.minusMinutes(30)
                : lessonStartTime;

        LocalTime resourceEndTime = isPickAndDrop
                ? lessonEndTime.plusMinutes(30)
                : lessonEndTime;

        LocalDate startDate = request.getStartDate();

        // =====================================================
        // 3️⃣ CUSTOMER SAME-DAY CHECK
        // =====================================================
        if (scheduleRepository.existsByCustomerIdAndDate(request.getCustomerId(), startDate)) {
            throw new RuntimeException("Customer already has a schedule on " + startDate);
        }

        // =====================================================
        // 4️⃣ CONFLICT CHECK (RESOURCE TIME ONLY)
        // =====================================================
        boolean conflict =
                scheduleRepository.existsByInstructorIdAndDateAndResourceStartTimeLessThanAndResourceEndTimeGreaterThan(
                        instructor.getId(), startDate, resourceEndTime, resourceStartTime
                ) ||
                        scheduleRepository.existsByVehicleVehicleNumberAndDateAndResourceStartTimeLessThanAndResourceEndTimeGreaterThan(
                                vehicle.getVehicleNumber(), startDate, resourceEndTime, resourceStartTime
                        );

        if (conflict) {
            throw new RuntimeException("Instructor or vehicle not available at preferred time on " + startDate);
        }

        // =====================================================
        // 5️⃣ SCHEDULE GENERATION
        // =====================================================
        List<Schedule> schedules = new ArrayList<>();
        LocalDate currentDate = startDate;

        for (int i = 0; i < course.getTotalDays(); i++) {

            // Skip day if customer already has a schedule
            if (scheduleRepository.existsByCustomerIdAndDate(request.getCustomerId(), currentDate)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            boolean dayConflict =
                    scheduleRepository.existsByInstructorIdAndDateAndResourceStartTimeLessThanAndResourceEndTimeGreaterThan(
                            instructor.getId(), currentDate, resourceEndTime, resourceStartTime
                    ) ||
                            scheduleRepository.existsByVehicleVehicleNumberAndDateAndResourceStartTimeLessThanAndResourceEndTimeGreaterThan(
                                    vehicle.getVehicleNumber(), currentDate, resourceEndTime, resourceStartTime
                            );

            if (dayConflict) {
                currentDate = findNextAvailableDate(
                        instructor.getId(),
                        vehicle.getVehicleNumber(),
                        resourceStartTime,
                        resourceEndTime,
                        currentDate
                );
            }

            Schedule schedule = Schedule.builder()
                    .course(course)
                    .customer(Customer.builder().id(request.getCustomerId()).build())
                    .instructor(instructor)
                    .vehicle(vehicle)
                    .date(currentDate)

                    // LESSON TIME
                    .startTime(lessonStartTime)
                    .endTime(lessonEndTime)

                    // RESOURCE TIME
                    .resourceStartTime(resourceStartTime)
                    .resourceEndTime(resourceEndTime)

                    .status(Schedule.ScheduleStatus.SCHEDULED)
                    .build();

            schedules.add(schedule);
            currentDate = currentDate.plusDays(1);
        }

        LocalDate actualEndDate = currentDate.minusDays(1);

        // =====================================================
        // 6️⃣ CUSTOMER CREATION
        // =====================================================
        Customer customer = Customer.builder()
                .name(request.getName())
                .contact(request.getContact())
                .address(request.getAddress())
                .course(course)
                .vehicle(vehicle)
                .assignedInstructor(instructor)
                .active(true)
                .deleted(false)
                .pickAndDrop(isPickAndDrop)
                .startDate(startDate)
                .endDate(actualEndDate)

                // Lesson preference (NOT resource time)
                .preferredStartTime(lessonStartTime)
                .preferredEndTime(lessonEndTime)
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        // =====================================================
        // 7️⃣ ATTACH CUSTOMER TO SCHEDULES
        // =====================================================
        schedules.forEach(schedule -> schedule.setCustomer(savedCustomer));
        scheduleRepository.saveAll(schedules);

        // =====================================================
        // 8️⃣ PAYMENT CREATION
        // =====================================================
        BigDecimal basePrice = course.getPrice();
        BigDecimal extraCharges = request.getExtraCharges() != null
                ? request.getExtraCharges()
                : BigDecimal.ZERO;

        BigDecimal discount = request.getDiscount() != null
                ? request.getDiscount()
                : BigDecimal.ZERO;

        BigDecimal totalPrice = basePrice.add(extraCharges).subtract(discount);
        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            totalPrice = BigDecimal.ZERO;
        }

        BigDecimal initialPayment = request.getInitialPayment() != null
                ? request.getInitialPayment()
                : BigDecimal.ZERO;

        boolean paymentCompleted = initialPayment.compareTo(totalPrice) >= 0;

        Payment payment = Payment.builder()
                .customer(savedCustomer)
                .basePrice(basePrice)
                .extraCharges(extraCharges)
                .discount(discount)
                .totalPrice(totalPrice)
                .initialPayment(initialPayment)
                .paymentCompleted(paymentCompleted)
                .paymentType(request.getPaymentType())
                .lastPaymentDate(LocalDate.now())
                .courseStartDate(savedCustomer.getStartDate())
                .courseEndDate(savedCustomer.getEndDate())
                .build();

        paymentRepository.save(payment);

        // =====================================================
        // 9️⃣ WHATSAPP NOTIFICATION (ACTUAL CUSTOMER)
        // =====================================================
        try {
            String rawContact = request.getContact();

            if (rawContact != null && !rawContact.isBlank()) {
                // Clean any accidental spaces/dashes and prepend 91
                String cleanNumber = rawContact.replaceAll("[^0-9]", "");
                String formattedNumber = "91" + cleanNumber;

                whatsappService.sendEnrollmentTemplate(formattedNumber, savedCustomer);
            }
        } catch (Exception e) {
            // Log the error but allow the method to return success
            // This prevents enrollment from failing if the WhatsApp API is down
            System.err.println("Failed to send WhatsApp message: " + e.getMessage());
        }

        return mapToDto(savedCustomer);
    }

    // ---------------- Schedule Management ----------------

//    public Schedule updateSchedule(Long scheduleId, LocalDate newDate, LocalTime newStartTime, LocalTime newEndTime) {
//        Schedule schedule = scheduleRepository.findById(scheduleId)
//                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
//
//        boolean conflict = scheduleRepository.existsByInstructorIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
//                schedule.getInstructor().getId(), newDate, newEndTime, newStartTime
//        ) || scheduleRepository.existsByVehicleVehicleNumberAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
//                schedule.getVehicle().getVehicleNumber(), newDate, newEndTime, newStartTime
//        ) || scheduleRepository.existsByCustomerIdAndDate(
//                schedule.getCustomer().getId(), newDate
//        );
//
//        if (conflict) {
//            throw new RuntimeException("Conflict detected, cannot reschedule to " + newDate);
//        }
//
//        schedule.setDate(newDate);
//        schedule.setStartTime(newStartTime);
//        schedule.setEndTime(newEndTime);
//        return scheduleRepository.save(schedule);
//    }
//
//    // ✅ Cancel and smart reschedule (1 day after last schedule or show available slots)
//    public Map<String, Object> cancelAndReschedule(Long scheduleId) {
//        Schedule schedule = scheduleRepository.findById(scheduleId)
//                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
//
//        schedule.setStatus(Schedule.ScheduleStatus.CANCELLED);
//        scheduleRepository.save(schedule);
//
//        Long customerId = schedule.getCustomer().getId();
//
//        // Find last scheduled date for this customer
//        LocalDate lastDate = scheduleRepository.findLastScheduledDateByCustomerId(customerId)
//                .orElse(LocalDate.now());
//
//        LocalDate newDate = lastDate.plusDays(1);
//        LocalTime startTime = schedule.getStartTime();
//        LocalTime endTime = schedule.getEndTime();
//
//        boolean conflict = scheduleRepository.existsByInstructorIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
//                schedule.getInstructor().getId(), newDate, endTime, startTime
//        ) || scheduleRepository.existsByVehicleVehicleNumberAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
//                schedule.getVehicle().getVehicleNumber(), newDate, endTime, startTime
//        );
//
//        Map<String, Object> response = new HashMap<>();
//
//        if (!conflict) {
//            // No conflict → reschedule one day later
//            Schedule newSchedule = Schedule.builder()
//                    .course(schedule.getCourse())
//                    .customer(schedule.getCustomer())
//                    .instructor(schedule.getInstructor())
//                    .vehicle(schedule.getVehicle())
//                    .date(newDate)
//                    .startTime(startTime)
//                    .endTime(endTime)
//                    .status(Schedule.ScheduleStatus.SCHEDULED)
//                    .build();
//
//            scheduleRepository.save(newSchedule);
//            response.put("message", "Schedule cancelled and rescheduled to " + newDate);
//            response.put("success", true);
//        } else {
//            // Conflict → show available slots
//            List<String> availableSlots = getAvailableSlots(
//                    schedule.getInstructor().getId(),
//                    schedule.getVehicle().getVehicleNumber(),
//                    newDate,
//                    1
//            );
//            response.put("message", "Conflict detected on " + newDate + ", choose one of the available slots");
//            response.put("availableSlots", availableSlots);
//            response.put("success", false);
//        }
//
//        return response;
//    }

    // ✅ Suggest available slots like your existing endpoint
//    private List<String> getAvailableSlots(Long instructorId, String vehicleNumber, LocalDate date, int slotDurationHours) {
//        List<String> available = new ArrayList<>();
//
//        LocalTime start = LocalTime.of(10, 0);
//        LocalTime end = LocalTime.of(18, 0);
//
//        while (start.plusHours(slotDurationHours).isBefore(end.plusSeconds(1))) {
//            LocalTime slotEnd = start.plusHours(slotDurationHours);
//
//            boolean conflict = scheduleRepository.existsByInstructorIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
//                    instructorId, date, slotEnd, start
//            ) || scheduleRepository.existsByVehicleVehicleNumberAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
//                    vehicleNumber, date, slotEnd, start
//            );
//
//            if (!conflict) {
//                available.add(start + " - " + slotEnd);
//            }
//
//            start = start.plusHours(slotDurationHours);
//        }
//
//        return available;
//    }

    // ---------------- Custom Business Logic ----------------

    /**
     * Finds active customers whose course endDate has passed and marks them inactive.
     * This relies on a JPA method in CustomerRepository:
     * 'List<Customer> findByActiveTrueAndEndDateBefore(LocalDate date);'
     */
    private void checkAndDeactivateExpiredCustomers() {
        LocalDate today = LocalDate.now();
        // Assuming findByActiveTrueAndEndDateBefore is available in CustomerRepository
        // List<Customer> expiredCustomers = customerRepository.findByActiveTrueAndEndDateBefore(today);

        // Using findAll() for demonstration if the custom JPA method is missing:
        List<Customer> expiredCustomers = customerRepository.findAll().stream()
                .filter(customer -> customer.isActive() &&
                        customer.getEndDate() != null &&
                        customer.getEndDate().isBefore(today))
                .collect(Collectors.toList());

        if (!expiredCustomers.isEmpty()) {
            expiredCustomers.forEach(customer -> {
                customer.setActive(false);
                // Schedules remain as the course was completed on time.
            });
            customerRepository.saveAll(expiredCustomers);
        }
    }


    // ---------------- Utility ----------------

    private LocalDate findNextAvailableDate(Long instructorId, String vehicleNumber,
                                            LocalTime startTime, LocalTime endTime, LocalDate fromDate) {
        LocalDate date = fromDate;
        while (true) {
            boolean conflict = scheduleRepository.existsByInstructorIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    instructorId, date, endTime, startTime
            ) || scheduleRepository.existsByVehicleVehicleNumberAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    vehicleNumber, date, endTime, startTime
            );
            if (!conflict) return date;
            date = date.plusDays(1);
        }
    }

    /**
     * Converts a Customer entity to CustomerResponseDto.
     * @param customer The Customer entity.
     * @return The CustomerResponseDto.
     */
    private CustomerResponseDto mapToDto(Customer customer) {
        return CustomerResponseDto.builder()
                .id(customer.getId())
                .name(customer.getName())
                .contact(customer.getContact())
                .courseId(customer.getCourse().getCourseId())
                .courseName(customer.getCourse().getCourseName())
                .vehicleNumber(customer.getVehicle().getVehicleNumber())
                .vehicleName(customer.getVehicle().getVehicleName())
                .assignedInstructorId(customer.getAssignedInstructor().getId())
                .assignedInstructorName(customer.getAssignedInstructor().getName())
                .startDate(customer.getStartDate())
                .endDate(customer.getEndDate())
                .address(customer.getAddress())
                .pickAndDrop(customer.isPickAndDrop())
                // Retrieve preferred time slot directly from Customer entity
                .preferredStartTime(customer.getPreferredStartTime())
                .preferredEndTime(customer.getPreferredEndTime())
                .active(customer.isActive())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }



    public LocalDate getCourseEndDate(Long customerId, Long courseId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + customerId));

        if (customer.getEndDate() == null) {
            throw new IllegalStateException("Customer course does not have a defined end date.");
        }
        return customer.getEndDate();
    }

    /**
     * Updates the customer's overall course end date.
     * Required by ScheduleService when a session is rescheduled past the current end date.
     */
    public void updateCourseEndDate(Long customerId, Long courseId, LocalDate newDate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + customerId));

        // Update the customer's overall course end date
        customer.setEndDate(newDate);
        customerRepository.save(customer);
    }

    public List<CustomerSchdeuleDTO> getCustomerSchedules(Long customerId) {

        List<Schedule> schedules = scheduleRepository.findByCustomerId(customerId);

        return schedules.stream().map(s -> {
            CustomerSchdeuleDTO dto = new CustomerSchdeuleDTO();

            dto.setId(s.getId());
            dto.setInstructorId(s.getInstructor().getId());
            dto.setInstructorName(s.getInstructor().getName());

            dto.setCustomerId(s.getCustomer().getId());
            dto.setCustomerName(s.getCustomer().getName());

            dto.setCourseId(s.getCourse().getCourseId());
            dto.setCourseName(s.getCourse().getCourseName());

            dto.setVehicleNumber(s.getVehicle().getVehicleNumber());
            dto.setVehicleName(s.getVehicle().getVehicleNumber());

            dto.setDate(s.getDate());
            dto.setStartTime(s.getStartTime());
            dto.setEndTime(s.getEndTime());
            dto.setStatus(s.getStatus());

            return dto;
        }).collect(Collectors.toList());
    }


    // Add this near your other @Value or fields
    @Value("${ors.api.key}")
    private String orsApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // Change this to your driving school's actual center point
    private final String DEFAULT_FROM_ADDRESS = "Pune Station, Maharashtra";

    /**
     * Calculates distance from center point to customer address using OpenRouteService.
     */
    public DistanceResponseDto calculateDistance(String toAddress) {
        if (toAddress == null || toAddress.isBlank()) {
            return new DistanceResponseDto(false, 0, "Address cannot be empty");
        }

        try {
            // 1. Geocode both addresses to get [Longitude, Latitude]
            double[] fromCoords = geocodeAddress(DEFAULT_FROM_ADDRESS);
            double[] toCoords = geocodeAddress(toAddress);

            // 2. Prepare Matrix API Request
            String url = "https://api.openrouteservice.org/v2/matrix/driving-car";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", orsApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "locations", List.of(fromCoords, toCoords),
                    "metrics", List.of("distance")
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 3. Call API
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> result = response.getBody();

            if (result != null && result.containsKey("distances")) {
                List<List<Double>> distances = (List<List<Double>>) result.get("distances");

                // Distance is in meters at [0][1] (from index 0 to index 1)
                double meters = distances.get(0).get(1);
                double km = Math.round((meters / 1000.0) * 100.0) / 100.0;

                return new DistanceResponseDto(true, km, "Distance calculated successfully");
            }

            return new DistanceResponseDto(false, 0, "Failed to retrieve distance from provider");

        } catch (Exception e) {
            return new DistanceResponseDto(false, 0, "Error: " + e.getMessage());
        }
    }

    /**
     * Helper to convert address string to coordinates
     */
    private double[] geocodeAddress(String address) throws Exception {
        String url = "https://api.openrouteservice.org/geocode/search?api_key=" + orsApiKey + "&text=" + address + "&size=1";

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        List<Map<String, Object>> features = (List<Map<String, Object>>) response.get("features");

        if (features == null || features.isEmpty()) {
            throw new RuntimeException("Could not find coordinates for: " + address);
        }

        Map<String, Object> firstResult = features.get(0);
        Map<String, Object> geometry = (Map<String, Object>) firstResult.get("geometry");
        List<Double> coords = (List<Double>) geometry.get("coordinates");

        // Returns [lon, lat]
        return new double[]{coords.get(0), coords.get(1)};
    }
}
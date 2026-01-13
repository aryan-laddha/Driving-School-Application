package com.example.drivingschool.service;

import com.example.drivingschool.dto.CustomerScheduleUpdateRequest;
import com.example.drivingschool.dto.ScheduleRequest;
import com.example.drivingschool.dto.ScheduleResponseDto;
import com.example.drivingschool.model.*;
import com.example.drivingschool.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final CourseRepository courseRepository;
    private final CustomerRepository customerRepository;

    private final CustomerService customerService;

    @Autowired
    private WhatsAppService whatsappService;

    // ---------------------------------------------------------------------
    //  CREATE SCHEDULE FOR MULTIPLE DAYS
    // ---------------------------------------------------------------------
    public String createSchedule(ScheduleRequest request) {

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        User instructor = userRepository.findById(request.getInstructorId())
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleNumber())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        LocalDate startDate = request.getStartDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusHours(course.getDurationPerDayHours());
        boolean isPickAndDrop = customer.isPickAndDrop();

        LocalTime resourceStart = calculateResourceStart(startTime, isPickAndDrop);
        LocalTime resourceEnd   = calculateResourceEnd(endTime, isPickAndDrop);

        List<Schedule> schedules = new ArrayList<>();

        // Validate availability for all days
        for (int i = 0; i < course.getTotalDays(); i++) {
            LocalDate scheduleDate = startDate.plusDays(i);

            boolean conflict =
                    !isInstructorAndVehicleAvailable(
                            instructor.getId(),
                            vehicle.getVehicleNumber(),
                            scheduleDate,
                            resourceStart,
                            resourceEnd,
                            null // <--- FIX: Pass null as no schedule is being excluded/updated
                    )
                            ||
                            scheduleRepository.existsByCustomerIdAndDate(customer.getId(), scheduleDate);

            if (conflict) {
                throw new RuntimeException(
                        "Conflict on " + scheduleDate + ". Entire schedule cannot be created.");
            }

            schedules.add(
                    Schedule.builder()
                            .course(course)
                            .customer(customer)
                            .instructor(instructor)
                            .vehicle(vehicle)
                            .date(scheduleDate)
                            .startTime(startTime)
                            .endTime(endTime)
                            .resourceStartTime(resourceStart)
                            .resourceEndTime(resourceEnd)
                            .status(Schedule.ScheduleStatus.SCHEDULED)
                            .build()
            );
        }

        scheduleRepository.saveAll(schedules);
        return "Schedule created successfully for " + course.getTotalDays() + " day(s)";
    }


    private LocalTime calculateResourceStart(LocalTime lessonStart, boolean pickAndDrop) {
        return pickAndDrop ? lessonStart.minusMinutes(30) : lessonStart;
    }

    private LocalTime calculateResourceEnd(LocalTime lessonEnd, boolean pickAndDrop) {
        return pickAndDrop ? lessonEnd.plusMinutes(30) : lessonEnd;
    }


    // ---------------------------------------------------------------------
    //  GET INSTRUCTOR SCHEDULE (No change needed)
    // ---------------------------------------------------------------------
    public List<ScheduleResponseDto> getInstructorSchedule(String username, LocalDate date) {
        User instructor = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        Long instructorId = instructor.getId();

        return scheduleRepository.findAll().stream()
                .filter(s -> s.getInstructor().getId().equals(instructorId)
                        && s.getDate().isEqual(date))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    //  GET VEHICLE SCHEDULE (No change needed)
    // ---------------------------------------------------------------------
    public List<ScheduleResponseDto> getVehicleSchedule(String vehicleNumber, LocalDate date) {
        return scheduleRepository.findAll().stream()
                .filter(s -> s.getVehicle().getVehicleNumber().equals(vehicleNumber)
                        && s.getDate().isEqual(date))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    //  DTO CONVERTER (No change needed)
    // ---------------------------------------------------------------------
    private ScheduleResponseDto convertToDto(Schedule schedule) {
        return ScheduleResponseDto.builder()
                .id(schedule.getId())
                .instructorId(schedule.getInstructor().getId())
                .instructorName(schedule.getInstructor().getName())
                .customerId(schedule.getCustomer().getId())
                .customerName(schedule.getCustomer().getName())
                .customerContact(schedule.getCustomer().getContact())
                .customerAddress(schedule.getCustomer().getAddress())
                .courseId(schedule.getCourse().getCourseId())
                .courseName(schedule.getCourse().getCourseName())
                .courseStartDate(schedule.getCustomer().getStartDate()) // ADDED
                .courseEndDate(schedule.getCustomer().getEndDate())   // ADDED
                .vehicleNumber(schedule.getVehicle().getVehicleNumber())
                .vehicleName(schedule.getVehicle().getVehicleName())
                .date(schedule.getDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .status(schedule.getStatus())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .pickAndDrop(schedule.getCustomer().isPickAndDrop())
                .build();
    }

    // ---------------------------------------------------------------------
    //  GET MULTI-DAY AVAILABLE SLOTS
    // ---------------------------------------------------------------------
    public List<String> getAvailableTimeSlots(Long instructorId, String vehicleNumber, LocalDate date,
                                              int slotDurationHours, Long courseId, boolean isPickAndDrop) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        List<String> availableSlots = new ArrayList<>();
        LocalTime startOfDay = LocalTime.of(7, 0); // Working starts at 7 AM
        LocalTime endOfDay = LocalTime.of(23, 0);   // Working ends at 11 PM

        int lessonDurationMinutes = slotDurationHours * 60;

        // t represents the START of the actual driving lesson
        for (LocalTime t = startOfDay; t.plusMinutes(lessonDurationMinutes).isBefore(endOfDay.plusSeconds(1)); t = t.plusMinutes(30)) {

            LocalTime lessonEnd = t.plusMinutes(lessonDurationMinutes);

            // Define the actual time the instructor/vehicle is busy
            // If pickAndDrop is true, we need them 30 mins before t and 30 mins after lessonEnd
            LocalTime resourceRequiredStart = isPickAndDrop ? t.minusMinutes(30) : t;
            LocalTime resourceRequiredEnd = isPickAndDrop ? lessonEnd.plusMinutes(30) : lessonEnd;

            // Validation: The BUFFERED time must stay within working hours
            // (e.g., if lesson starts at 7:00, pick-up at 6:30 is invalid)
            if (resourceRequiredStart.isBefore(startOfDay) || resourceRequiredEnd.isAfter(endOfDay)) {
                continue;
            }

            boolean availableForEntireCourse = true;

            // Verify availability across all required days for the course
            for (int i = 0; i < course.getTotalDays(); i++) {
                LocalDate checkDate = date.plusDays(i);

                // IMPORTANT: We check the database for the RESOURCE window (120 mins),
                // not just the LESSON window (60 mins).
                if (!isInstructorAndVehicleAvailable(instructorId, vehicleNumber, checkDate,
                        resourceRequiredStart, resourceRequiredEnd, null)) {
                    availableForEntireCourse = false;
                    break;
                }
            }

            if (availableForEntireCourse) {
                // Option A: Return only the Lesson Time (What the student pays for)
                // availableSlots.add(t + " - " + lessonEnd);

                // Option B: Return the full Service Time (Clearer for scheduling)
                if (isPickAndDrop) {
                    availableSlots.add(t + " - " + lessonEnd + " (Travel: " + resourceRequiredStart + " to " + resourceRequiredEnd + ")");
                } else {
                    availableSlots.add(t + " - " + lessonEnd);
                }
            }
        }
        return availableSlots;
    }
    // ---------------------------------------------------------------------
    //  UPDATE STATUS (No change needed)
    // ---------------------------------------------------------------------
    public Schedule updateScheduleStatus(Long scheduleId, Schedule.ScheduleStatus status) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        Schedule updatedSchedule = scheduleRepository.save(schedule);

        Customer customer = schedule.getCustomer();
        String customerNumber = normalizeWhatsappNumber(customer.getContact());

        schedule.setStatus(status);
        if (status == Schedule.ScheduleStatus.COMPLETED) {
            whatsappService.sendSessionCompletionMessage(
                    customerNumber,
                    updatedSchedule
            );

            boolean workRemaining = scheduleRepository.existsByCustomerIdAndStatusIn(
                    schedule.getCustomer().getId(),
                    List.of(Schedule.ScheduleStatus.SCHEDULED, Schedule.ScheduleStatus.RESCHEDULED)
            );

            // 4. If workRemaining is FALSE, it means all sessions are now COMPLETED (or Cancelled)
            if (!workRemaining) {

                // Mark the customer as finished
                customer.setActive(false);
                customer.setCompleted(true);
                customerRepository.save(customer);

                // 5. Trigger the Feedback Template (Hi {{1}}, service on {{2}} is closed...)
                whatsappService.sendFeedbackMessage(
                        customerNumber,
                        customer.getName(),
                        LocalDate.now()
                );
            }


        }
        return updatedSchedule;
    }






    // ---------------------------------------------------------------------
    //  CHECK AVAILABILITY (UPDATED SIGNATURE)
    // ---------------------------------------------------------------------
    private boolean isInstructorAndVehicleAvailable(Long instructorId, String vehicleNumber,
                                                    LocalDate date, LocalTime resourceStart, LocalTime resourceEnd, Long excludedScheduleId) { // <-- CORRECTED SIGNATURE

        // The repository calls now pass the excludedScheduleId
        boolean instructorFree =
                scheduleRepository.findOverlappingSchedulesByUser(instructorId, date, resourceStart, resourceEnd, excludedScheduleId)
                        .isEmpty();

        boolean vehicleFree =
                scheduleRepository.findOverlappingSchedulesByVehicle(vehicleNumber, date, resourceStart, resourceEnd, excludedScheduleId)
                        .isEmpty();

        return instructorFree && vehicleFree;
    }

    // ---------------------------------------------------------------------
    //  CANCEL SCHEDULE (No change needed)
    // ---------------------------------------------------------------------
    public Schedule cancelSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        schedule.setStatus(Schedule.ScheduleStatus.CANCELLED);
        Schedule cancelledSchedule = scheduleRepository.save(schedule);

        Customer customer = schedule.getCustomer();
        String customerNumber = normalizeWhatsappNumber(customer.getContact());

        try {


            whatsappService.sendCancellationMessage(
                    customerNumber,
                    cancelledSchedule.getCustomer().getName(),
                    cancelledSchedule.getDate().toString(),
                    cancelledSchedule.getStartTime().toString()
            );
        } catch (Exception e) {
            // Log error but don't break the DB transaction
            System.err.println("Failed to send WhatsApp cancellation: " + e.getMessage());
        }
        return cancelledSchedule;
    }

    // ---------------------------------------------------------------------
    //  RESCHEDULE CANCELLED SCHEDULE
    // ---------------------------------------------------------------------
    public Schedule rescheduleSchedule(Long scheduleId, LocalDate newDate, LocalTime newStartTime) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        if (schedule.getStatus() != Schedule.ScheduleStatus.CANCELLED) {
            throw new RuntimeException("Only cancelled schedules can be rescheduled");
        }

        LocalTime newEndTime = newStartTime.plusHours(schedule.getCourse().getDurationPerDayHours());

        boolean conflict =
                !isInstructorAndVehicleAvailable(
                        schedule.getInstructor().getId(),
                        schedule.getVehicle().getVehicleNumber(),
                        newDate,
                        newStartTime,
                        newEndTime,
                        scheduleId // <--- FIX: Exclude current schedule
                )
                        ||
                        scheduleRepository.existsByCustomerIdAndDate(schedule.getCustomer().getId(), newDate);

        if (conflict) {
            throw new RuntimeException("Conflict exists, choose another time slot");
        }

        schedule.setDate(newDate);
        schedule.setStartTime(newStartTime);
        schedule.setEndTime(newEndTime);
        schedule.setStatus(Schedule.ScheduleStatus.SCHEDULED);

        return scheduleRepository.save(schedule);
    }

    // ---------------------------------------------------------------------
    //  CANCEL + AUTO RESCHEDULE
    // ---------------------------------------------------------------------
    public Map<String, Object> cancelAndAutoReschedule(Long scheduleId, LocalDate newDate, LocalTime newStartTime, LocalTime newEndTime) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        // --- 1. GET CUSTOMER PREFERENCES ---
        Customer customer = schedule.getCustomer();
        boolean isPickAndDrop = customer.isPickAndDrop();

        Long instructorId = schedule.getInstructor().getId();
        String vehicleNumber = schedule.getVehicle().getVehicleNumber();
        int durationHours = schedule.getCourse().getDurationPerDayHours();
        Long customerId = customer.getId();
        Long courseId = schedule.getCourse().getCourseId();

        // --- 2. CALCULATE RESOURCE WINDOW (BUFFERS) ---
        // Resource Window = Time the instructor/vehicle is physically occupied
        LocalTime resourceStart = isPickAndDrop ? newStartTime.minusMinutes(30) : newStartTime;
        LocalTime resourceEnd = isPickAndDrop ? newEndTime.plusMinutes(30) : newEndTime;

        LocalTime workStart = LocalTime.of(7, 0);
        LocalTime workEnd = LocalTime.of(23, 0);

        // --- 3. CONFLICT CHECK ---
        boolean outsideWorkHours = resourceStart.isBefore(workStart) || resourceEnd.isAfter(workEnd);

        // Note: isInstructorAndVehicleAvailable MUST check against resourceStartTime/End columns in DB
        boolean instructorVehicleConflict = outsideWorkHours || !isInstructorAndVehicleAvailable(
                instructorId, vehicleNumber, newDate, resourceStart, resourceEnd, scheduleId
        );

        boolean customerConflict = !scheduleRepository.findOverlappingSchedulesByCustomer(
                customerId, newDate, resourceStart, resourceEnd, scheduleId
        ).isEmpty();

        Map<String, Object> response = new HashMap<>();

        if (instructorVehicleConflict || customerConflict) {
            List<String> availableSlots = getSingleDayAvailableSlots(
                    instructorId, vehicleNumber, newDate, durationHours, isPickAndDrop
            );

            String msg = outsideWorkHours ? "Requested time involves Pick-up/Drop-off outside working hours (07:00 - 23:00)."
                    : "Conflict detected: The requested time slot or travel buffer is already booked.";

            response.put("message", msg);
            response.put("availableSlots", availableSlots);
            response.put("success", false);
            return response;
        }

        // --- 4. NO CONFLICT: PROCEED WITH UPDATE ---
        LocalDate oldDate = schedule.getDate();
        String oldTimeStr = schedule.getStartTime() + " - " + schedule.getEndTime();

        // CORE FIX: Always store actual Lesson Time in startTime/endTime
        // and Busy Window in resourceStartTime/resourceEndTime
        schedule.setDate(newDate);
        schedule.setStartTime(newStartTime);
        schedule.setEndTime(newEndTime);
        schedule.setResourceStartTime(resourceStart);
        schedule.setResourceEndTime(resourceEnd);

        if (newDate.isEqual(oldDate)) {
            response.put("message", "Time slot updated successfully.");
        } else {
            schedule.setStatus(Schedule.ScheduleStatus.RESCHEDULED);

            // Logic to update course end date if rescheduled to a later date
            try {
                Optional<LocalDate> lastDate = scheduleRepository.findLastScheduledDateByCustomerId(customerId);
                if (lastDate.isPresent() && lastDate.get().isAfter(customer.getEndDate())) {
                    customer.setEndDate(lastDate.get());
                    customerRepository.save(customer);
                }
            } catch (Exception e) {
                System.err.println("End date update failed: " + e.getMessage());
            }
        }

        scheduleRepository.save(schedule);

        // --- 5. RESPONSE FORMATTING ---
        response.put("success", true);
        response.put("newDate", schedule.getDate());
        response.put("lessonTime", newStartTime + " - " + newEndTime);
        response.put("serviceWindow", resourceStart + " - " + resourceEnd);
        response.put("isPickAndDrop", isPickAndDrop);

        String detailedMsg = isPickAndDrop
                ? "Rescheduled: Pickup at " + resourceStart + ", Lesson starts at " + newStartTime
                : "Rescheduled: Lesson from " + newStartTime + " to " + newEndTime;
        response.put("displayMessage", detailedMsg);


        String customerNumber = normalizeWhatsappNumber(customer.getContact());

        // --- 6. WHATSAPP NOTIFICATION ---
        try {

            // WhatsApp clearly distinguishes between Pickup and Lesson
            String timeDisplay = isPickAndDrop
                    ? "Pickup: " + resourceStart + " | Lesson: " + newStartTime + "-" + newEndTime
                    : newStartTime + " - " + newEndTime;

            whatsappService.sendRescheduleMessage(
                    customerNumber,
                    customer.getName(),
                    oldDate.toString(),
                    oldTimeStr,
                    newDate.toString(),
                    timeDisplay
            );
        } catch (Exception e) {
            System.err.println("WhatsApp Notification Failed: " + e.getMessage());
        }

        return response;
    }// ---------------------------------------------------------------------
    //  SINGLE DAY SLOT CHECKER
    // ---------------------------------------------------------------------
    private List<String> getSingleDayAvailableSlots(Long instructorId, String vehicleNumber,
                                                    LocalDate date, int durationHours, boolean isPickAndDrop) {

        List<String> slots = new ArrayList<>();
        LocalTime startOfDay = LocalTime.of(7, 0); // Start of working hours
        LocalTime endOfDay = LocalTime.of(22, 0);   // End of working hou   rs

        // Duration of the actual lesson
        int lessonMinutes = durationHours * 60;

        // We iterate by 30-minute increments to find better start times
        for (LocalTime t = startOfDay; t.plusMinutes(lessonMinutes).isBefore(endOfDay.plusSeconds(1)); t = t.plusMinutes(30)) {

            LocalTime lessonEnd = t.plusMinutes(lessonMinutes);

            // --- Pick & Drop Buffer Logic ---
            // resourceStart/End is the total time the instructor is 'busy'
            LocalTime resourceStart = isPickAndDrop ? t.minusMinutes(30) : t;
            LocalTime resourceEnd = isPickAndDrop ? lessonEnd.plusMinutes(30) : lessonEnd;

            // Safety: Ensure the pickup doesn't start before work or drop-off end after work
            if (resourceStart.isBefore(startOfDay) || resourceEnd.isAfter(endOfDay)) {
                continue;
            }

            // Check availability for the FULL resource window (Lesson + 60 mins total buffer)
            if (isInstructorAndVehicleAvailable(instructorId, vehicleNumber, date, resourceStart, resourceEnd, null)) {

                // We return the actual LESSON time to the user,
                // but we've verified the buffer is available in the DB.
                slots.add(t + " - " + lessonEnd);
            }
        }
        return slots;
    }

    // ---------------------------------------------------------------------
    //  UPDATE TIME FOR ALL PENDING DAYS OF A VEHICLE
    // ---------------------------------------------------------------------
    @Transactional
    public Map<String, Object> updateTimeForAllPendingDays(String vehicleNumber, LocalTime newStartTime) {

        // Fetch all pending schedules for the vehicle
        List<Schedule> pending = scheduleRepository.findAll().stream()
                .filter(s -> s.getVehicle().getVehicleNumber().equals(vehicleNumber)
                        && s.getStatus() == Schedule.ScheduleStatus.SCHEDULED)
                .collect(Collectors.toList());

        if (pending.isEmpty()) {
            throw new RuntimeException("No pending schedules found for this vehicle");
        }

        Schedule firstSchedule = pending.get(0);
        String customerName = firstSchedule.getCustomer().getName();

        int updated = 0;
        int skipped = 0;
        List<LocalDate> skippedDates = new ArrayList<>();


        String customerNumber = normalizeWhatsappNumber(firstSchedule.getCustomer().getContact());


        for (Schedule s : pending) {

            // LESSON end time based on course duration
            LocalTime newEndTime = newStartTime.plusHours(s.getCourse().getDurationPerDayHours());

            // RESOURCE window calculation
            boolean isPickAndDrop = s.getCustomer().isPickAndDrop();
            LocalTime resourceStart = isPickAndDrop ? newStartTime.minusMinutes(30) : newStartTime;
            LocalTime resourceEnd = isPickAndDrop ? newEndTime.plusMinutes(30) : newEndTime;

            // Check if the resource time is available
            boolean conflict = !isInstructorAndVehicleAvailable(
                    s.getInstructor().getId(),
                    vehicleNumber,
                    s.getDate(),
                    resourceStart,
                    resourceEnd,
                    s.getId() // Exclude current schedule
            );

            if (conflict) {
                skipped++;
                skippedDates.add(s.getDate());
                continue;
            }

            // Update schedule
            s.setStartTime(newStartTime);
            s.setEndTime(newEndTime);
            s.setResourceStartTime(resourceStart);
            s.setResourceEndTime(resourceEnd);

            scheduleRepository.save(s);
            updated++;
        }

        // WhatsApp notification (optional)
        if (updated > 0) {
            try {
                String timeRange = newStartTime + " - " + pending.get(0).getEndTime();
                if (pending.get(0).getCustomer().isPickAndDrop()) {
                    timeRange += " (Travel: " + pending.get(0).getResourceStartTime() +
                            " to " + pending.get(0).getResourceEndTime() + ")";
                }
                whatsappService.sendBulkCourseUpdate(
                        customerNumber,
                        customerName,
                        timeRange
                );
            } catch (Exception e) {
                System.err.println("WhatsApp failed: " + e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Update completed");
        response.put("updatedCount", updated);
        response.put("skippedCount", skipped);
        response.put("skippedDates", skippedDates);
        response.put("success", true);

        return response;
    }

    // ---------------------------------------------------------------------
    //  UPDATE TIME FOR A SINGLE DAY
    // ---------------------------------------------------------------------
    public Map<String, Object> updateScheduleTime(Long scheduleId, LocalTime newStartTime) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        Customer customer = schedule.getCustomer();
        boolean isPickAndDrop = customer.isPickAndDrop(); // Check flag from DB

        // Capture "Old" data for the WhatsApp message
        String oldDateStr = schedule.getDate().toString();
        String oldTimeStr = schedule.getStartTime().toString() + " - " + schedule.getEndTime().toString();

        Course course = schedule.getCourse();
        int durationHours = course.getDurationPerDayHours();
        LocalTime newEndTime = newStartTime.plusHours(durationHours);

        LocalTime resourceStart = calculateResourceStart(newStartTime, isPickAndDrop);
        LocalTime resourceEnd   = calculateResourceEnd(newEndTime, isPickAndDrop);

        // Check if buffers stay within working hours (e.g., 7 AM to 11 PM)
        LocalTime workStart = LocalTime.of(7, 0);
        LocalTime workEnd = LocalTime.of(23, 0);

        boolean outsideWorkingHours = resourceStart.isBefore(workStart) || resourceEnd.isAfter(workEnd);

        // Conflict check (Checking the RESOURCE window, excluding current scheduleId)
        boolean conflict = outsideWorkingHours || !isInstructorAndVehicleAvailable(
                schedule.getInstructor().getId(),
                schedule.getVehicle().getVehicleNumber(),
                schedule.getDate(),
                resourceStart,
                resourceEnd,
                scheduleId
        );

        Map<String, Object> response = new HashMap<>();

        if (conflict) {
            // Pass the isPickAndDrop flag to get compatible alternative slots
            List<String> availableSlots = getSingleDayAvailableSlots(
                    schedule.getInstructor().getId(),
                    schedule.getVehicle().getVehicleNumber(),
                    schedule.getDate(),
                    durationHours,
                    isPickAndDrop // Logic updated to handle buffer requirements
            );
            response.put("message", outsideWorkingHours ? "Pick-up/Drop-off outside working hours" : "Conflict: Time not available");
            response.put("availableSlots", availableSlots);
            response.put("success", false);
            return response;
        }

        // Update the schedule with the LESSON times
        // (Note: Usually, the Schedule table stores the lesson time,
        // but the conflict check reserves the buffer)
        schedule.setStartTime(newStartTime);
        schedule.setEndTime(newEndTime);
        schedule.setResourceStartTime(resourceStart);
        schedule.setResourceEndTime(resourceEnd);


        scheduleRepository.save(schedule);


        String customerNumber = normalizeWhatsappNumber(customer.getContact());



        // --- WhatsApp Notification ---
        try {
            String newTimeStr = isPickAndDrop
                    ? newStartTime + " (Pick-up: " + resourceStart + ")"
                    : newStartTime.toString();

            whatsappService.sendRescheduleMessage(
                    customerNumber,
                    customer.getName(),
                    oldDateStr,
                    oldTimeStr,
                    oldDateStr,
                    newTimeStr + " - " + newEndTime
            );
        } catch (Exception e) {
            System.err.println("WhatsApp Error: " + e.getMessage());
        }

        response.put("message", "Schedule updated successfully");
        response.put("newTime", newStartTime + " - " + newEndTime);
        response.put("isPickAndDrop", isPickAndDrop);
        response.put("success", true);
        return response;
    }


    public List<ScheduleResponseDto> getFilteredDashboardSchedule(LocalDate date, Long instructorId, String vehicleNumber, Long courseId, Schedule.ScheduleStatus status ) {
        return scheduleRepository.findAll().stream()
                .filter(s -> s.getDate().isEqual(date))

                // Filter by Instructor ID
                .filter(s -> instructorId == null || s.getInstructor().getId().equals(instructorId))

                // Filter by Vehicle Number
                .filter(s -> vehicleNumber == null || (s.getVehicle() != null && s.getVehicle().getVehicleNumber().equals(vehicleNumber)))

                // Filter by Course ID (NEW LOGIC)
                .filter(s -> courseId == null || (s.getCourse() != null && s.getCourse().getCourseId().equals(courseId)))

                // Filter by Status
                .filter(s -> status == null || s.getStatus() == status)

                .map(this::convertToDto) // Ensure this method maps the new course date fields!
                .collect(Collectors.toList());
    }


    @Transactional
    public Customer deleteUpcomingSchedulesAndMarkInactive(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + customerId));

        // 1. Define statuses to be deleted
        List<Schedule.ScheduleStatus> deletableStatuses = List.of(
                Schedule.ScheduleStatus.SCHEDULED,
                Schedule.ScheduleStatus.RESCHEDULED
        );

        // 2. Find schedules to be deleted (today or future dates)
        LocalDate today = LocalDate.now();
        List<Schedule> schedulesToDelete = scheduleRepository.findByCustomerIdAndDateGreaterThanEqualAndStatusIn(
                customerId,
                today,
                deletableStatuses
        );

        // 3. Delete the found schedules
        scheduleRepository.deleteAll(schedulesToDelete);

        // 4. Mark the customer as inactive
        customer.setActive(false);
        return customerRepository.save(customer);
    }


    @Transactional
    public String bulkUpdateCustomerSchedules(CustomerScheduleUpdateRequest request) {

        LocalDate today = LocalDate.now();

        // 1. Fetch Customer
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // 2. Resolve Target Instructor & Vehicle
        User targetInstructor = (request.getNewInstructorId() != null)
                ? userRepository.findById(request.getNewInstructorId())
                .orElseThrow(() -> new RuntimeException("Instructor not found"))
                : customer.getAssignedInstructor();

        Vehicle targetVehicle = (request.getNewVehicleNumber() != null)
                ? vehicleRepository.findById(request.getNewVehicleNumber())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"))
                : customer.getVehicle();

        // 3. Resolve Lesson Start Time
        LocalTime lessonStartTime = (request.getNewStartTime() != null)
                ? request.getNewStartTime()
                : customer.getPreferredStartTime();

        boolean isPickAndDrop = (request.getPickAndDrop() != null)
                ? request.getPickAndDrop()
                : customer.isPickAndDrop();

        // 4. Calculate Lesson & Resource Times
        Course course = customer.getCourse();
        int durationHours = course.getDurationPerDayHours();

        LocalTime lessonEndTime = lessonStartTime.plusHours(durationHours);

        LocalTime resourceStartTime = isPickAndDrop
                ? lessonStartTime.minusMinutes(30)
                : lessonStartTime;

        LocalTime resourceEndTime = isPickAndDrop
                ? lessonEndTime.plusMinutes(30)
                : lessonEndTime;

        // 5. Fetch Upcoming Schedules
        List<Schedule> upcomingSchedules =
                scheduleRepository.findByCustomerIdAndDateGreaterThanEqualAndStatusIn(
                        customer.getId(),
                        today,
                        List.of(
                                Schedule.ScheduleStatus.SCHEDULED,
                                Schedule.ScheduleStatus.RESCHEDULED
                        )
                );

        if (upcomingSchedules.isEmpty()) {
            throw new RuntimeException("No upcoming schedules found to update.");
        }

        // 6. VALIDATION PHASE (RESOURCE WINDOW CHECK)
        for (Schedule schedule : upcomingSchedules) {
            boolean available = isInstructorAndVehicleAvailable(
                    targetInstructor.getId(),
                    targetVehicle.getVehicleNumber(),
                    schedule.getDate(),
                    resourceStartTime,
                    resourceEndTime,
                    schedule.getId()
            );

            if (!available) {
                throw new RuntimeException(
                        "Conflict on " + schedule.getDate() +
                                " for resource time " + resourceStartTime + " - " + resourceEndTime
                );
            }
        }

        // 7. EXECUTION PHASE (SAVE BOTH LESSON & RESOURCE TIMES)
        for (Schedule schedule : upcomingSchedules) {
            schedule.setInstructor(targetInstructor);
            schedule.setVehicle(targetVehicle);

            // LESSON
            schedule.setStartTime(lessonStartTime);
            schedule.setEndTime(lessonEndTime);

            // RESOURCE
            schedule.setResourceStartTime(resourceStartTime);
            schedule.setResourceEndTime(resourceEndTime);
        }

        scheduleRepository.saveAll(upcomingSchedules);

        // 8. Update Customer Preferences
        customer.setAssignedInstructor(targetInstructor);
        customer.setVehicle(targetVehicle);
        customer.setPickAndDrop(isPickAndDrop);
        customer.setPreferredStartTime(lessonStartTime);
        customer.setPreferredEndTime(lessonEndTime);
        customerRepository.save(customer);

        String customerNumber = normalizeWhatsappNumber(customer.getContact());

        // 9. WhatsApp Notification (Soft)
        try {
            String msgTime = lessonStartTime + " - " + lessonEndTime;
            if (isPickAndDrop) {
                msgTime += " (Pickup: " + resourceStartTime + ")";
            }
            whatsappService.sendBulkCourseUpdate(
                    customerNumber,
                    customer.getName(),
                    msgTime
            );
        } catch (Exception e) {
            System.err.println("WhatsApp Log: " + e.getMessage());
        }

        return "Successfully updated schedules using resource-based timing.";
    }

    public List<String> getAvailableSlotsForBulkUpdate(CustomerScheduleUpdateRequest request) {

        LocalDate today = LocalDate.now();

        // 1. Fetch Customer
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // 2. Resolve Instructor & Vehicle
        User targetInstructor = (request.getNewInstructorId() != null)
                ? userRepository.findById(request.getNewInstructorId())
                .orElseThrow(() -> new RuntimeException("Instructor not found"))
                : customer.getAssignedInstructor();

        Vehicle targetVehicle = (request.getNewVehicleNumber() != null)
                ? vehicleRepository.findById(request.getNewVehicleNumber())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"))
                : customer.getVehicle();

        boolean isPickAndDrop = (request.getPickAndDrop() != null)
                ? request.getPickAndDrop()
                : customer.isPickAndDrop();

        Course course = customer.getCourse();
        int durationHours = course.getDurationPerDayHours();

        // 3. Fetch Upcoming Schedules
        List<Schedule> upcomingSchedules =
                scheduleRepository.findByCustomerIdAndDateGreaterThanEqualAndStatusIn(
                        customer.getId(),
                        today,
                        List.of(
                                Schedule.ScheduleStatus.SCHEDULED,
                                Schedule.ScheduleStatus.RESCHEDULED
                        )
                );

        if (upcomingSchedules.isEmpty()) {
            throw new RuntimeException("No upcoming schedules found.");
        }

        // 4. Generate Slots
        List<String> validSlots = new ArrayList<>();

        LocalTime workStart = LocalTime.of(7, 0);
        LocalTime workEnd = LocalTime.of(23, 0);

        for (LocalTime lessonStart = workStart;
             lessonStart.plusHours(durationHours).isBefore(workEnd.plusSeconds(1));
             lessonStart = lessonStart.plusMinutes(30)) {

            LocalTime lessonEnd = lessonStart.plusHours(durationHours);

            LocalTime resourceStart = isPickAndDrop
                    ? lessonStart.minusMinutes(30)
                    : lessonStart;

            LocalTime resourceEnd = isPickAndDrop
                    ? lessonEnd.plusMinutes(30)
                    : lessonEnd;

            // Boundary Check
            if (resourceStart.isBefore(workStart) || resourceEnd.isAfter(workEnd)) {
                continue;
            }

            // Must be valid for ALL upcoming dates
            boolean validForAll = true;

            for (Schedule schedule : upcomingSchedules) {
                if (!isInstructorAndVehicleAvailable(
                        targetInstructor.getId(),
                        targetVehicle.getVehicleNumber(),
                        schedule.getDate(),
                        resourceStart,
                        resourceEnd,
                        schedule.getId()
                )) {
                    validForAll = false;
                    break;
                }
            }

            if (validForAll) {
                if (isPickAndDrop) {
                    validSlots.add(
                            lessonStart + " - " + lessonEnd +
                                    " (Travel: " + resourceStart + " to " + resourceEnd + ")"
                    );
                } else {
                    validSlots.add(lessonStart + " - " + lessonEnd);
                }
            }
        }

        return validSlots;
    }

    private String normalizeWhatsappNumber(String phone) {

        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Customer phone number not found");
        }

        phone = phone.replaceAll("\\s+", "");

        // If already has country code
        if (phone.startsWith("91") && phone.length() == 12) {
            return phone;
        }

        if (phone.startsWith("+91")) {
            return phone.substring(1);
        }

        // Assume Indian number without country code
        if (phone.length() == 10) {
            return "91" + phone;
        }

        throw new RuntimeException("Invalid phone number format: " + phone);
    }


}
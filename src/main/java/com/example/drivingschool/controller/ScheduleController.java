package com.example.drivingschool.controller;

import com.example.drivingschool.dto.CustomerScheduleUpdateRequest;
import com.example.drivingschool.dto.RescheduleRequestDto;
import com.example.drivingschool.dto.ScheduleRequest;
import com.example.drivingschool.dto.ScheduleResponseDto;
import com.example.drivingschool.model.Schedule;
import com.example.drivingschool.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/create")
    public ResponseEntity<String> createSchedule(@RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.createSchedule(request));
    }

    @GetMapping("/instructor/{username}")
    public ResponseEntity<List<ScheduleResponseDto>> getInstructorSchedule( // 👈 Change List<Schedule> to List<ScheduleResponseDto>
                                                                            @PathVariable String username,
                                                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(scheduleService.getInstructorSchedule(username, date));
    }

    @GetMapping("/vehicle/{vehicleNumber}")
    public ResponseEntity<List<ScheduleResponseDto>> getVehicleSchedule( // 👈 Change List<Schedule> to List<ScheduleResponseDto>
                                                                         @PathVariable String vehicleNumber,
                                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(scheduleService.getVehicleSchedule(vehicleNumber, date));
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<String>> getAvailableSlots(
            @RequestParam Long instructorId,
            @RequestParam String vehicleNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int slotDurationHours,
            @RequestParam Long courseId,
            @RequestParam Boolean isPickAndDrop// 🚨 ADDED: Required to get the course total days
    ) {
        return ResponseEntity.ok(scheduleService.getAvailableTimeSlots(
                instructorId,
                vehicleNumber,
                date,
                slotDurationHours,
                courseId,
                isPickAndDrop// 🚨 PASSING NEW PARAMETER
        ));
    }

    @PatchMapping("/update-status/{scheduleId}")
    public ResponseEntity<Schedule> updateScheduleStatus(
            @PathVariable Long scheduleId,
            @RequestParam Schedule.ScheduleStatus status
    ) {
        return ResponseEntity.ok(scheduleService.updateScheduleStatus(scheduleId, status));
    }

    @PatchMapping("/reschedule")
    public ResponseEntity<Schedule> rescheduleSchedule(@RequestBody RescheduleRequestDto request) {
        Schedule updated = scheduleService.rescheduleSchedule(
                request.getScheduleId(),
                request.getNewDate(),
                request.getNewStartTime()
        );
        return ResponseEntity.ok(updated);
    }


    @PatchMapping("/cancel/{scheduleId}")
    public ResponseEntity<Schedule> cancelSchedule(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(scheduleService.cancelSchedule(scheduleId));
    }

    @PatchMapping("/cancel-reschedule/{scheduleId}")
    public ResponseEntity<?> cancelAndReschedule(
            @PathVariable Long scheduleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newStartTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newEndTime
    ) {
        return ResponseEntity.ok(scheduleService.cancelAndAutoReschedule(  scheduleId, newDate, newStartTime, newEndTime));
    }


    @PatchMapping("/update-time/all")
    public ResponseEntity<?> updateTimeForAllPendingDays(
            @RequestParam String vehicleNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newStartTime
    ) {
        return ResponseEntity.ok(scheduleService.updateTimeForAllPendingDays(vehicleNumber, newStartTime));
    }

    @PatchMapping("/update-time/{scheduleId}")
    public ResponseEntity<?> updateScheduleTime(
            @PathVariable Long scheduleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newStartTime
    ) {
        return ResponseEntity.ok(scheduleService.updateScheduleTime(scheduleId, newStartTime));
    }
    @GetMapping("/allschedule")
    public ResponseEntity<List<ScheduleResponseDto>> getDashboardSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long instructorId,
            @RequestParam(required = false) String vehicleNumber,
            @RequestParam(required = false) Long courseId, // ADDED: Course Filter Parameter
            @RequestParam(required = false) Schedule.ScheduleStatus status
    ) {
        // UPDATED: Pass the new courseId parameter to the service method
        return ResponseEntity.ok(scheduleService.getFilteredDashboardSchedule(
                date,
                instructorId,
                vehicleNumber,
                courseId, // Passed here
                status
        ));
    }

    @PatchMapping("/cancel-all-upcoming")
    public ResponseEntity<?> cancelAllUpcomingSchedules(@RequestParam Long customerId) {
        try {
            // Call the new service method that performs deletion
            scheduleService.deleteUpcomingSchedulesAndMarkInactive(customerId);

            String message = String.format("Successfully deleted all upcoming schedules for Customer ID %d and marked them inactive.", customerId);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/bulk-update")
    public ResponseEntity<?> updateCustomerSchedule(@RequestBody CustomerScheduleUpdateRequest request) {
        try {
            String message = scheduleService.bulkUpdateCustomerSchedules(request);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            // Return 409 Conflict or 400 Bad Request depending on your preference
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PostMapping("/available-slots-for-update")
    public ResponseEntity<List<String>> getAvailableSlotsForUpdate(
            @RequestBody CustomerScheduleUpdateRequest request) {

        List<String> slots = scheduleService.getAvailableSlotsForBulkUpdate(request);
        return ResponseEntity.ok(slots);
    }

}

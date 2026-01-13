package com.example.drivingschool.repository;

import com.example.drivingschool.model.Customer;
import com.example.drivingschool.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // --- 1. CORE OVERLAP LOGIC (THE "GOLD STANDARD") ---
    // These methods check if any existing BUSY WINDOW (resourceStartTime to resourceEndTime)
    // overlaps with the REQUESTED window.

    @Query("SELECT s FROM Schedule s WHERE s.instructor.id = :userId AND s.date = :date " +
            "AND s.status IN ('SCHEDULED', 'RESCHEDULED') " +
            "AND (:startTime < s.resourceEndTime AND :endTime > s.resourceStartTime) " +
            "AND s.id != COALESCE(:excludedScheduleId, -1)")
    List<Schedule> findOverlappingSchedulesByUser(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime, // Pass the Resource Start
            @Param("endTime") LocalTime endTime,     // Pass the Resource End
            @Param("excludedScheduleId") Long excludedScheduleId
    );

    @Query("SELECT s FROM Schedule s WHERE s.vehicle.vehicleNumber = :vehicleNumber AND s.date = :date " +
            "AND s.status IN ('SCHEDULED', 'RESCHEDULED') " +
            "AND (:startTime < s.resourceEndTime AND :endTime > s.resourceStartTime) " +
            "AND s.id != COALESCE(:excludedScheduleId, -1)")
    List<Schedule> findOverlappingSchedulesByVehicle(
            @Param("vehicleNumber") String vehicleNumber,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludedScheduleId") Long excludedScheduleId
    );

    @Query("SELECT s FROM Schedule s WHERE s.customer.id = :customerId AND s.date = :date " +
            "AND s.status IN ('SCHEDULED', 'RESCHEDULED') " +
            "AND (:startTime < s.resourceEndTime AND :endTime > s.resourceStartTime) " +
            "AND s.id != COALESCE(:excludedScheduleId, -1)")
    List<Schedule> findOverlappingSchedulesByCustomer(
            @Param("customerId") Long customerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludedScheduleId") Long excludedScheduleId
    );

    // --- 2. UPDATED RESOURCE-AWARE CONFLICT CHECKS ---
    // Replaces legacy methods to ensure enrollment uses buffers

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
            "WHERE s.instructor.id = :instructorId " +
            "AND s.date = :date " +
            "AND s.status IN ('SCHEDULED', 'RESCHEDULED') " +
            "AND s.resourceStartTime < :resEnd AND s.resourceEndTime > :resStart")
    boolean existsResourceConflictInstructor(
            @Param("instructorId") Long instructorId,
            @Param("date") LocalDate date,
            @Param("resStart") LocalTime resStart,
            @Param("resEnd") LocalTime resEnd
    );

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
            "WHERE s.vehicle.vehicleNumber = :vehicleNumber " +
            "AND s.date = :date " +
            "AND s.status IN ('SCHEDULED', 'RESCHEDULED') " +
            "AND s.resourceStartTime < :resEnd AND s.resourceEndTime > :resStart")
    boolean existsResourceConflictVehicle(
            @Param("vehicleNumber") String vehicleNumber,
            @Param("date") LocalDate date,
            @Param("resStart") LocalTime resStart,
            @Param("resEnd") LocalTime resEnd
    );

    // --- 3. UTILITY & FETCH METHODS ---

    List<Schedule> findByVehicleVehicleNumberAndDate(String vehicleNumber, LocalDate date);

    List<Schedule> findByInstructorIdAndDate(Long userId, LocalDate date);

    @Query("SELECT MAX(s.date) FROM Schedule s WHERE s.customer.id = :customerId AND s.status != 'CANCELLED'")
    Optional<LocalDate> findLastScheduledDateByCustomerId(@Param("customerId") Long customerId);

    List<Schedule> findByCustomerIdAndDateGreaterThanEqualAndStatusIn(
            Long customerId,
            LocalDate date,
            Collection<Schedule.ScheduleStatus> statuses
    );

    // For enrollment today/future checks
    boolean existsByCustomerIdAndDateAndStatusIn(Long customerId, LocalDate date, Collection<Schedule.ScheduleStatus> statuses);

    // Fetching by customer for reporting/profile
    List<Schedule> findByCustomerId(Long customerId);

    List<Schedule> findByCustomerIdAndDateAfterAndStatus(
            Long customerId, LocalDate date, Schedule.ScheduleStatus status
    );

    void deleteByCustomer(Customer customer);

    boolean existsByCustomerIdAndDate(Long customerId, LocalDate date);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
            "WHERE s.instructor.id = :instructorId " +
            "AND s.date = :date " +
            "AND s.resourceStartTime < :resEnd AND s.resourceEndTime > :resStart")
    boolean existsByInstructorIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Long instructorId, LocalDate date, LocalTime resourceEndTime, LocalTime resourceStartTime
    );

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
            "WHERE s.vehicle.vehicleNumber = :vehicleNumber " +
            "AND s.date = :date " +
            "AND s.resourceStartTime < :resEnd  AND s.resourceEndTime  > :resStart")
    boolean existsByVehicleVehicleNumberAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            String vehicleNumber, LocalDate date, LocalTime resourceEndTime, LocalTime resourceStartTime
    );

    boolean existsByVehicleVehicleNumberAndDateAndResourceStartTimeLessThanAndResourceEndTimeGreaterThan(
            String vehicleNumber,
            LocalDate date,
            LocalTime resourceEndTime,
            LocalTime resourceStartTime
    );
    boolean existsByInstructorIdAndDateAndResourceStartTimeLessThanAndResourceEndTimeGreaterThan(
            Long instructorId,
            LocalDate date,
            LocalTime resourceEndTime,
            LocalTime resourceStartTime
    );


    List<Schedule> findByDate(LocalDate date);

    /**
     * Alternative: Fetch schedules within a date range (Optional optimization)
     * Useful if you want to fetch all 7 days in one query and filter in memory.
     */
    List<Schedule> findByDateBetween(LocalDate startDate, LocalDate endDate);


    boolean existsByCustomerIdAndStatusIn(Long customerId, List<Schedule.ScheduleStatus> statuses);

    @Query("SELECT COUNT(DISTINCT s.customer.id) FROM Schedule s WHERE s.date = CURRENT_DATE AND s.status = 'SCHEDULED'")
    long countLiveSchedules();


}
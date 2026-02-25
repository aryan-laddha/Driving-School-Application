package com.example.drivingschool.controller;

import com.example.drivingschool.model.Query;
import com.example.drivingschool.service.QueryService;
import com.example.drivingschool.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Keep for protected endpoints
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/queries")
public class QueryController {

    @Autowired
    private QueryService queryService;

    // 1. PUBLIC ENDPOINT: Query Submission
    // NO @PreAuthorize is required here. Access will be configured in Spring Security.
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Query>> submitQuery(@RequestBody Query query) {
        try {
            // Set the submission date and initial flags on the server side
            query.setSubmissionDate(LocalDateTime.now());
            query.setFollowUpRequired(true);
            query.setResolved(false);

            Query savedQuery = queryService.saveQuery(query);

            return new ResponseEntity<>(
                    new ApiResponse<>(true, "Your query has been submitted successfully.", savedQuery),
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse<>(false, "Failed to submit query: " + e.getMessage(), null),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // 2. PROTECTED ENDPOINT: View All Queries (Requires authentication)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Query>>> getAllQueries() {
        try {
            List<Query> queries = queryService.getAllQueries();

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Successfully retrieved " + queries.size() + " queries.", queries)
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse<>(false, "Failed to retrieve queries: " + e.getMessage(), null),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // 3. PROTECTED ENDPOINT: Update Query Status (Requires authentication)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Query>> updateQueryStatus(@PathVariable Long id, @RequestBody Query queryDetails) {
        try {
            Query updatedQuery = queryService.updateQuery(id, queryDetails);

            if (updatedQuery != null) {
                return ResponseEntity.ok(
                        new ApiResponse<>(true, "Query status updated successfully.", updatedQuery)
                );
            } else {
                return new ResponseEntity<>(
                        new ApiResponse<>(false, "Query with ID " + id + " not found.", null),
                        HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse<>(false, "Failed to update query status: " + e.getMessage(), null),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
package com.example.drivingschool.controller;

import com.example.drivingschool.dto.*;
import com.example.drivingschool.service.CustomerService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ApiResponse<CustomerResponseDto> addCustomer(@RequestBody CustomerRequestDto request) {
        CustomerResponseDto customer = customerService.addCustomer(request);
        return new ApiResponse<>(true, "Customer added successfully", customer);
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponseDto> updateCustomer(@PathVariable Long id,
                                                           @RequestBody CustomerRequestDto request) {
        CustomerResponseDto customer = customerService.updateCustomer(id, request);
        return new ApiResponse<>(true, "Customer updated successfully", customer);
    }

    @GetMapping
    public ApiResponse<List<CustomerResponseDto>> getAllCustomers() {
        List<CustomerResponseDto> customers = customerService.getAllCustomers();
        return new ApiResponse<>(true, "All customers fetched", customers);
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponseDto> getCustomerById(@PathVariable Long id) {
        CustomerResponseDto customer = customerService.getCustomerById(id);
        return new ApiResponse<>(true, "Customer fetched successfully", customer);
    }

    @DeleteMapping("/soft-delete/{id}")
    public ApiResponse<String> softDeleteCustomer(@PathVariable Long id) {
        customerService.softDeleteCustomer(id);
        return new ApiResponse<>(true, "Customer soft-deleted successfully", null);
    }

    @DeleteMapping("/{id}/hard-delete") // Changed path to include {id}
    public ApiResponse<String> hardDeleteCustomer(@PathVariable Long id) {
        try {
            customerService.hardDeleteCustomer(id);
            return new ApiResponse<>(true, "Customer and all associated schedules permanently deleted.", null);
        } catch (EntityNotFoundException e) {
            // You should handle errors appropriately
            return new ApiResponse<>(false, "Customer not found with ID: " + id, null);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Failed to hard delete customer.", null);
        }
    }

    @PostMapping("/enroll")
    public ApiResponse<CustomerResponseDto> enrollCustomer(@RequestBody CustomerEnrollRequestDto request) {
        CustomerResponseDto customer = customerService.enrollCustomer(request);
        return new ApiResponse<>(true, "Customer enrolled and schedule created successfully", customer);
    }

    @GetMapping("/{customerId}/schedules")
    public ApiResponse<List<CustomerSchdeuleDTO>> getCustomerSchedules(@PathVariable Long customerId) {
        List<CustomerSchdeuleDTO> schedules = customerService.getCustomerSchedules(customerId);
        return new ApiResponse<>(true, "Customer schedules fetched successfully", schedules);
    }


    @PostMapping("/calculate-distance")
    public ApiResponse<DistanceResponseDto> calculateDistance(@RequestBody DistanceRequestDto request) {
        try {
            DistanceResponseDto response = customerService.calculateDistance(request.getAddress());
            if (response.isSuccess()) {
                return new ApiResponse<>(true, "Distance calculated successfully", response);
            } else {
                return new ApiResponse<>(false, "Calculation failed: " + response.getMessage(), null);
            }
        } catch (Exception e) {
            return new ApiResponse<>(false, "Error: " + e.getMessage(), null);
        }
    }




}

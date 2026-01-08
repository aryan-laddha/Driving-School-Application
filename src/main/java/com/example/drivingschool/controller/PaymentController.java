package com.example.drivingschool.controller;

import com.example.drivingschool.dto.ApiResponse;
import com.example.drivingschool.dto.PaymentAdjustmentRequestDto;
import com.example.drivingschool.dto.PaymentRequestDto;
import com.example.drivingschool.dto.PaymentResponseDto;
import com.example.drivingschool.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // --------------------- CREATE PAYMENT ----------------------
    @PostMapping
    public ApiResponse<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto request) {
        PaymentResponseDto response = paymentService.createPayment(request);
        return new ApiResponse<>(true, "Payment created successfully", response);
    }

    // --------------------- GET PAYMENT BY ID ----------------------
    @GetMapping("/{id}")
    public ApiResponse<PaymentResponseDto> getById(@PathVariable Long id) {
        return new ApiResponse<>(true, "Payment fetched", paymentService.getPaymentById(id));
    }

    // --------------------- ALL PAYMENTS ----------------------
    @GetMapping
    public ApiResponse<List<PaymentResponseDto>> getAll() {
        return new ApiResponse<>(true, "All payments fetched", paymentService.getAllPayments());
    }

    // --------------------- CUSTOMER PAYMENTS ----------------------
    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<PaymentResponseDto>> getByCustomer(@PathVariable Long customerId) {
        return new ApiResponse<>(true, "Customer payments fetched", paymentService.getPaymentsByCustomer(customerId));
    }

    // --------------------- REMAINING (PENDING) PAYMENTS ----------------------
    @GetMapping("/pending")
    public ApiResponse<List<PaymentResponseDto>> getPendingPayments() {
        return new ApiResponse<>(true, "Pending payments fetched", paymentService.getRemainingPayments());
    }
    // Inside PaymentController.java

    @PostMapping("/{id}/send-reminder")
    public ApiResponse<String> sendPaymentReminder(@PathVariable Long id) {
        String status = paymentService.sendReminder(id);
        return new ApiResponse<>(true, status, null);
    }

    // --------------------- ADJUST PAYMENT AMOUNT ----------------------
    @PutMapping("/{id}/adjust-price")
    public ApiResponse<PaymentResponseDto> adjustPaymentPrice(
            @PathVariable Long id,
            @RequestBody PaymentAdjustmentRequestDto request) {
        PaymentResponseDto response = paymentService.adjustPayment(id, request);
        return new ApiResponse<>(true, "Payment amounts adjusted successfully", response);
    }



}

package com.example.drivingschool.service;

import com.example.drivingschool.dto.PaymentAdjustmentRequestDto;
import com.example.drivingschool.dto.PaymentRequestDto;
import com.example.drivingschool.dto.PaymentResponseDto;

import com.example.drivingschool.model.Customer;
import com.example.drivingschool.model.Payment;
import com.example.drivingschool.repository.CustomerRepository;
import com.example.drivingschool.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    private final WhatsAppService whatsappService;

    // ===================================================
    // CREATE NEW PAYMENT
    // ===================================================
    public PaymentResponseDto createPayment(PaymentRequestDto request) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        BigDecimal basePrice = customer.getCourse().getPrice();
        BigDecimal extraCharges = request.getExtraCharges() != null ? request.getExtraCharges() : BigDecimal.ZERO;
        BigDecimal discount = request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO;

        BigDecimal total = basePrice.add(extraCharges).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        BigDecimal initialPaid = request.getInitialPayment() != null ? request.getInitialPayment() : BigDecimal.ZERO;

        boolean isCompleted = initialPaid.compareTo(total) >= 0;

        Payment payment = Payment.builder()
                .customer(customer)
                .basePrice(basePrice)
                .extraCharges(extraCharges)
                .discount(discount)
                .totalPrice(total)
                .initialPayment(initialPaid)
                .paymentCompleted(isCompleted)
                .paymentType(request.getPaymentType())
                .lastPaymentDate(request.getLastPaymentDate() != null ? request.getLastPaymentDate() : LocalDate.now())
                .courseStartDate(customer.getStartDate())
                .courseEndDate(customer.getEndDate())
                .build();

        Payment saved = paymentRepository.save(payment);

        return mapToDto(saved);
    }


    // ===================================================
    // GET PAYMENT BY ID
    // ===================================================
    public PaymentResponseDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));

        return mapToDto(payment);
    }


    // ===================================================
    // GET ALL PAYMENTS
    // ===================================================
    public List<PaymentResponseDto> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }


    // ===================================================
    // GET PAYMENTS BY CUSTOMER
    // ===================================================
    public List<PaymentResponseDto> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }


    // ===================================================
    // GET REMAINING PAYMENTS (paymentCompleted = false)
    // ===================================================
    public List<PaymentResponseDto> getRemainingPayments() {
        return paymentRepository.findByPaymentCompletedFalse()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }


    // ===================================================
    // DTO MAPPER
    // ===================================================
    private PaymentResponseDto mapToDto(Payment p) {
        return PaymentResponseDto.builder()
                .id(p.getId())
                .customerId(p.getCustomer().getId())
                .customerName(p.getCustomer().getName())
                .customerContact(p.getCustomer().getContact())
                .basePrice(p.getBasePrice())
                .extraCharges(p.getExtraCharges())
                .discount(p.getDiscount())
                .totalPrice(p.getTotalPrice())
                .initialPayment(p.getInitialPayment())
                .paymentCompleted(p.isPaymentCompleted())
                .paymentType(p.getPaymentType())
                .lastPaymentDate(p.getLastPaymentDate())
                .courseStartDate(p.getCourseStartDate())
                .courseEndDate(p.getCourseEndDate())
                .createdAt(p.getCreatedAt())
                .build();
    }

    public String sendReminder(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment record not found"));

        if (payment.isPaymentCompleted()) {
            return "Payment is already completed. No reminder needed.";
        }

        BigDecimal pendingAmount = payment.getTotalPrice().subtract(payment.getInitialPayment());

        // Format the phone number: Ensure it includes the country code (e.g., 91 for India)
//        String phone = payment.getCustomer().getContact();
        Customer customer = payment.getCustomer();
        String customerNumber = normalizeWhatsappNumber(customer.getContact());

        whatsappService.sendPaymentReminder(
                customerNumber,
                payment.getCustomer().getName(),
                pendingAmount.toString(),
                payment.getTotalPrice().toString(),
                payment.getCustomer().getEndDate()
        );

        return "Reminder sent to " + payment.getCustomer().getName();
    }



    /* Utility */
    private BigDecimal zeroIfNull(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }


    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
    @Transactional
    public PaymentResponseDto adjustPayment(Long paymentId, PaymentAdjustmentRequestDto request) {
        // 1. Fetch record
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        // 2. Map and Sanitize Inputs
        BigDecimal base = request.getNewBasePrice() != null ? request.getNewBasePrice() : BigDecimal.ZERO;
        BigDecimal extra = request.getNewExtraCharges() != null ? request.getNewExtraCharges() : BigDecimal.ZERO;
        BigDecimal discount = request.getNewDiscount() != null ? request.getNewDiscount() : BigDecimal.ZERO;

        // 3. Recalculate Total Price: (Base + Extra) - Discount
        BigDecimal calculatedTotal = base.add(extra).subtract(discount);

        // Safety: Ensure total is never negative
        if (calculatedTotal.compareTo(BigDecimal.ZERO) < 0) {
            calculatedTotal = BigDecimal.ZERO;
        }

        // 4. Update Database Fields
        payment.setBasePrice(base);
        payment.setExtraCharges(extra);
        payment.setDiscount(discount);
        payment.setTotalPrice(calculatedTotal);

        // 5. Update Payment Status (Logic-driven)
        // Compare total with what they already paid (initialPayment)
        BigDecimal alreadyPaid = payment.getInitialPayment() != null ? payment.getInitialPayment() : BigDecimal.ZERO;

        // If the new total is less than or equal to what was already paid, it's completed
        boolean isNowPaidInFull = alreadyPaid.compareTo(calculatedTotal) >= 0;
        payment.setPaymentCompleted(isNowPaidInFull);

        // 6. Persist
        Payment updatedPayment = paymentRepository.save(payment);
        return mapToDto(updatedPayment);
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

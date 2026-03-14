package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.entity.Payout;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.smart_booking_system.repository.PayoutRepository;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới được chia tiền
public class AdminPayoutController {

    private final PayoutService payoutService;
    private final UserRepository userRepository;
    private final PayoutRepository payoutRepository;

    // 1. API: Tính doanh thu tháng trước cho 1 Owner (Tạo Payout PENDING)
    @PostMapping("/calculate/{ownerId}")
    public ResponseEntity<?> calculatePayout(@PathVariable String ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Owner"));

        // Lấy tháng trước
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusMonths(1).withDayOfMonth(1); // Ngày 1 tháng trước
        LocalDate endDate = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth()); // Ngày cuối tháng trước

        Payout payout = payoutService.calculateAndCreatePayout(owner, startDate, endDate);

        if (payout == null) {
            return ResponseEntity.ok(ApiResponse.success("Chủ khách sạn này không có doanh thu trong kỳ", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Đã tính toán doanh thu thành công", payout));
    }

    // 2. API: Bấm nút Bắn tiền thật sự qua Stripe
    @PostMapping("/process/{payoutId}")
    public ResponseEntity<?> processPayout(@PathVariable Long payoutId) {
        try {
            Payout processed = payoutService.processPayout(payoutId);
            return ResponseEntity.ok(ApiResponse.success("Đã xử lý thanh toán: " + processed.getStatus(), processed));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    @GetMapping
    public ResponseEntity<?> getAllPayouts() {
        return ResponseEntity.ok(ApiResponse.success("Thành công", payoutRepository.findAll()));
    }
}
package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.RefundSubmitDTO;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 1. Khách thanh toán (Gọi sau khi Gateway trả về success hoặc nút "Thanh toán ngay")
    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<?> submitPayment(
            @PathVariable int bookingId,
            @RequestParam(defaultValue = "Online Banking") String method,
            @RequestParam(required = false) String note
    ) {
        return ResponseEntity.ok(paymentService.submitPayment(bookingId, note, method));
    }

    // 2. Lịch sử giao dịch của User đang login
    @GetMapping("/my-history")
    public ResponseEntity<?> getMyHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ✅ SỬA LẠI ĐOẠN NÀY
        String userId;
        if (auth.getPrincipal() instanceof CustomUserDetails) {
            // Lấy ID thật sự từ token (đã được map vào CustomUserDetails)
            userId = ((CustomUserDetails) auth.getPrincipal()).getUserId();
        } else {
            // Fallback nếu có lỗi (thường ít khi vào đây nếu đã qua filter)
            userId = auth.getName();
        }

        return ResponseEntity.ok(paymentService.getUserTransactionHistory(userId));
    }

    // 3. Gửi yêu cầu hoàn tiền (User)
    @PostMapping("/{bookingId}/refund-request")
    public ResponseEntity<?> requestRefund(@PathVariable int bookingId, @RequestBody RefundSubmitDTO req) {
        return ResponseEntity.ok(paymentService.requestRefundByUser(bookingId, req));
    }

    // 4. Admin xử lý hoàn tiền
    @PutMapping("/refund-process/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> processRefund(
            @PathVariable int requestId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String note
    ) {
        return ResponseEntity.ok(paymentService.processRefund(requestId, approve, note));
    }

    // 5. Admin xem tất cả giao dịch
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllTransactions() {
        return ResponseEntity.ok(paymentService.getAllTransactions());
    }
}
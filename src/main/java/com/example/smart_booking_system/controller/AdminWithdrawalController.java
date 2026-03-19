package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.WithdrawalRejectRequest;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.WithdrawalResponseDTO;
import com.example.smart_booking_system.entity.WithdrawalRequest;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/withdrawals")
@RequiredArgsConstructor
public class AdminWithdrawalController {

    private final WithdrawalService withdrawalService;

    // 1. API: Admin lấy toàn bộ danh sách yêu cầu rút tiền
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getAllRequests() {
        List<WithdrawalRequest> requests = withdrawalService.getAllWithdrawalRequests();
        List<WithdrawalResponseDTO> responseList = requests.stream().map(WithdrawalResponseDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành công", responseList));
    }

    // 2. API: Admin duyệt lệnh (Gọi Stripe bắn tiền)
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> approveWithdrawal(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            WithdrawalRequest request = withdrawalService.approveWithdrawal(requestId, userDetails.getUserId());
            return ResponseEntity.ok(ApiResponse.success("Đã duyệt lệnh rút tiền và chuyển khoản qua Stripe thành công!", new WithdrawalResponseDTO(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 3. API: Admin từ chối lệnh
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> rejectWithdrawal(
            @PathVariable Long requestId,
            @RequestBody WithdrawalRejectRequest payload,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            WithdrawalRequest request = withdrawalService.rejectWithdrawal(requestId, payload.getAdminNote(), userDetails.getUserId());
            return ResponseEntity.ok(ApiResponse.success("Đã từ chối lệnh rút tiền và hoàn số dư cho Chủ nhà.", new WithdrawalResponseDTO(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
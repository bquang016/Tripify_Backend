package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.WithdrawalSubmitRequest;
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
import com.example.smart_booking_system.service.WalletService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner/withdrawals")
@RequiredArgsConstructor
public class OwnerWithdrawalController {

    private final WithdrawalService withdrawalService;
    private final WalletService walletService;

    // 1. API: Owner tạo lệnh rút tiền
    @PostMapping("/request")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<?>> requestWithdrawal(
            @RequestBody WithdrawalSubmitRequest payload,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            WithdrawalRequest request = withdrawalService.requestWithdrawal(userDetails.getUserId(), payload.getAmount());
            return ResponseEntity.ok(ApiResponse.success("Yêu cầu rút tiền đã được gửi thành công", new WithdrawalResponseDTO(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 2. API: Owner xem lịch sử rút tiền
    @GetMapping("/history")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<?>> getWithdrawalHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<WithdrawalRequest> history = withdrawalService.getWithdrawalHistoryByOwner(userDetails.getUserId());
        List<WithdrawalResponseDTO> responseList = history.stream().map(WithdrawalResponseDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thành công", responseList));
    }

    // 3. API: Lấy thông tin số dư Ví
    @GetMapping("/wallet")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<?>> getMyWallet(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            var wallet = walletService.getWalletByOwnerId(userDetails.getUserId());
            // Trả về thẳng một Map cho nhanh, không cần tạo DTO mới
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("availableBalance", wallet.getAvailableBalance());
            data.put("pendingBalance", wallet.getPendingBalance());
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin ví thành công", data));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
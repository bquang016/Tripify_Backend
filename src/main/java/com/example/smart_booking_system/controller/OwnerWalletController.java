package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.owner.PayoutSettingsDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/owner/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerWalletController {

    private final WalletService walletService;

    @GetMapping("/payout-settings")
    public ResponseEntity<ApiResponse<PayoutSettingsDTO>> getPayoutSettings(Principal principal) {
        String ownerEmail = principal.getName();
        PayoutSettingsDTO settings = walletService.getPayoutSettings(ownerEmail);

        // Sử dụng hàm static success() có sẵn trong ApiResponse của bạn
        return ResponseEntity.ok(ApiResponse.success("Fetched payout settings successfully", settings));
    }

    @PutMapping("/payout-settings")
    public ResponseEntity<ApiResponse<PayoutSettingsDTO>> updatePayoutSettings(
            Principal principal,
            @RequestBody PayoutSettingsDTO request) {

        String ownerEmail = principal.getName();
        PayoutSettingsDTO updatedSettings = walletService.updatePayoutSettings(ownerEmail, request);

        return ResponseEntity.ok(ApiResponse.success("Payout settings updated successfully", updatedSettings));
    }

    @DeleteMapping("/payout-settings/{type}")
    public ResponseEntity<ApiResponse<PayoutSettingsDTO>> deletePaymentMethod(
            Principal principal,
            @PathVariable String type) {

        String ownerEmail = principal.getName();
        PayoutSettingsDTO updatedSettings = walletService.deletePaymentMethod(ownerEmail, type);

        return ResponseEntity.ok(ApiResponse.success("Deleted payment method successfully", updatedSettings));
    }
}
package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.ExchangeRateUpdateDTO;
import com.example.smart_booking_system.dto.request.MaintenanceModeUpdateDTO;
import com.example.smart_booking_system.dto.request.SystemSettingUpdateDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.ExchangeRateResponseDTO;
import com.example.smart_booking_system.dto.response.SystemSettingResponseDTO;
import com.example.smart_booking_system.service.FileStorageService;
import com.example.smart_booking_system.service.SystemSettingService;
import com.example.smart_booking_system.security.CheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@CheckPermission("SYSTEM_MANAGE")
public class AdminSystemSettingController {

    private final SystemSettingService systemSettingService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<ApiResponse<SystemSettingResponseDTO>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(systemSettingService.getSettings()));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<SystemSettingResponseDTO>> updateSettings(@RequestBody SystemSettingUpdateDTO updateDTO) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình thành công", systemSettingService.updateSettings(updateDTO)));
    }

    @PostMapping("/logo")
    public ResponseEntity<ApiResponse<String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        String key = fileStorageService.storeImageFile(file);
        String signedUrl = fileStorageService.generateSignedUrl(key);
        systemSettingService.updateLogo(signedUrl);
        return ResponseEntity.ok(ApiResponse.success("Tải lên logo thành công", signedUrl));
    }

    @DeleteMapping("/logo")
    public ResponseEntity<ApiResponse<Void>> deleteLogo() {
        systemSettingService.deleteLogo();
        return ResponseEntity.ok(ApiResponse.success("Xóa logo thành công"));
    }

    @GetMapping("/exchange-rates")
    public ResponseEntity<ApiResponse<List<ExchangeRateResponseDTO>>> getExchangeRates() {
        return ResponseEntity.ok(ApiResponse.success(systemSettingService.getExchangeRates()));
    }

    @PatchMapping("/exchange-rates")
    public ResponseEntity<ApiResponse<ExchangeRateResponseDTO>> updateExchangeRate(@RequestBody ExchangeRateUpdateDTO updateDTO) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tỷ giá thành công", systemSettingService.updateExchangeRate(updateDTO)));
    }

    @PostMapping("/maintenance")
    public ResponseEntity<ApiResponse<Void>> updateMaintenanceMode(@RequestBody MaintenanceModeUpdateDTO updateDTO) {
        systemSettingService.updateMaintenanceMode(updateDTO);
        String status = updateDTO.getIsEnabled() ? "bật" : "tắt";
        return ResponseEntity.ok(ApiResponse.success("Đã " + status + " chế độ bảo trì"));
    }
}

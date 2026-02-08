package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.FullOnboardingRequest;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.OwnerOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/owner/onboarding")
@RequiredArgsConstructor
public class OwnerOnboardingController {

    private final OwnerOnboardingService ownerOnboardingService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/register-full", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> registerFull(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestPart("data") String jsonData,
            // Files from Step 1
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart("cccdFront") MultipartFile cccdFront,
            @RequestPart("cccdBack") MultipartFile cccdBack,
            // Files from Step 2
            @RequestPart("propertyImages") List<MultipartFile> propertyImages,
            @RequestPart("businessLicenseImage") MultipartFile businessLicenseImage,
            @RequestPart(name = "unitImages", required = false) List<MultipartFile> unitImages
    ) {
        try {
            objectMapper.registerModule(new JavaTimeModule());
            FullOnboardingRequest request = objectMapper.readValue(jsonData, FullOnboardingRequest.class);

            ownerOnboardingService.registerFull(
                    currentUser.getUserId(),
                    request,
                    avatar,
                    cccdFront,
                    cccdBack,
                    propertyImages,
                    businessLicenseImage,
                    unitImages
            );

            return ResponseEntity.ok(ApiResponse.success("Đăng ký thông tin đối tác thành công. Vui lòng chờ duyệt.", null));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi xử lý dữ liệu: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi hệ thống: " + e.getMessage()));
        }
    }
}


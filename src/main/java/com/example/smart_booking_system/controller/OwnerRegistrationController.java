package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.OwnerApplicationRequest;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.OwnerRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner-registration")
@RequiredArgsConstructor
public class OwnerRegistrationController {

    private final OwnerRegistrationService ownerRegistrationService;
    private final ObjectMapper objectMapper;

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Void>> submitApplication(
            @RequestHeader("Authorization") String temporaryToken,
            @RequestPart("data") String jsonData,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart("cccdFront") MultipartFile cccdFront,
            @RequestPart("cccdBack") MultipartFile cccdBack,
            @RequestPart("businessLicenseImage") MultipartFile businessLicenseImage,
            @RequestPart("propertyImages") List<MultipartFile> propertyImages,
            @RequestPart(name = "unitImages", required = false) List<MultipartFile> unitImages) {
        
        try {
            OwnerApplicationRequest request = objectMapper.readValue(jsonData, OwnerApplicationRequest.class);
            
            ownerRegistrationService.submitApplication(
                    temporaryToken,
                    request,
                    avatar,
                    cccdFront,
                    cccdBack,
                    propertyImages,
                    businessLicenseImage,
                    unitImages
            );
            
            return ResponseEntity.ok(ApiResponse.success("Application submitted successfully. Please wait for approval.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

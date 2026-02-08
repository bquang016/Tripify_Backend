package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.FullOnboardingRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OwnerOnboardingService {
    void registerFull(
            String userId,
            FullOnboardingRequest request,
            MultipartFile avatar,
            MultipartFile cccdFront,
            MultipartFile cccdBack,
            List<MultipartFile> propertyImages,
            MultipartFile businessLicenseImage,
            List<MultipartFile> unitImages
    );
}

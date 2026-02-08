package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.OwnerApplicationRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OwnerRegistrationService {
    void submitApplication(
            String temporaryToken,
            OwnerApplicationRequest request,
            MultipartFile avatar,
            MultipartFile cccdFront,
            MultipartFile cccdBack,
            List<MultipartFile> propertyImages,
            MultipartFile businessLicenseImage,
            List<MultipartFile> unitImages
    );
}

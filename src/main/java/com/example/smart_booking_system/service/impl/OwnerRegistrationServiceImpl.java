package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.internal.OwnerApplicationData;
import com.example.smart_booking_system.dto.request.OwnerApplicationRequest;
import com.example.smart_booking_system.entity.OwnerApplication;
import com.example.smart_booking_system.enums.ApplicationStatus;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.InternalServerException;
import com.example.smart_booking_system.exception.ConflictException;
import com.example.smart_booking_system.repository.OwnerApplicationRepository;
import com.example.smart_booking_system.security.JwtTokenProvider;
import com.example.smart_booking_system.service.EmailService;
import com.example.smart_booking_system.service.FileStorageService;
import com.example.smart_booking_system.service.OwnerRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerRegistrationServiceImpl implements OwnerRegistrationService {

    private final OwnerApplicationRepository ownerApplicationRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void submitApplication(
            String temporaryToken,
            OwnerApplicationRequest request,
            MultipartFile avatar,
            MultipartFile cccdFront,
            MultipartFile cccdBack,
            List<MultipartFile> propertyImages,
            MultipartFile businessLicenseImage,
            List<MultipartFile> unitImages
    ) {
        if (temporaryToken == null || !temporaryToken.startsWith("Bearer ")) {
            throw new BadRequestException("Missing or invalid temporary token.");
        }
        String token = temporaryToken.substring(7).trim();
        if (token.isEmpty()) {
            throw new BadRequestException("Temporary token is empty.");
        }

        String email;
        try {
            // getEmailFromToken will throw ExpiredJwtException, SignatureException, etc. if token is invalid
            email = jwtTokenProvider.getEmailFromToken(token);
            
            String type = jwtTokenProvider.getClaimFromToken(token, "type", String.class);
            if (!"temporary".equals(type)) {
                throw new BadRequestException("Invalid token type. Expected a temporary registration token.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Temporary token error: " + e.getMessage());
        }

        if (email == null) {
            throw new BadRequestException("Invalid temporary token payload.");
        }

        String ownerIdForPaths = email.replaceAll("[^a-zA-Z0-9]", "_");

        String avatarUrl = (avatar != null && !avatar.isEmpty())
                ? fileStorageService.storeImageFile(avatar, "avatars/" + ownerIdForPaths) : null;
        String cccdFrontUrl = fileStorageService.storeImageFile(cccdFront, "cccd/" + ownerIdForPaths);
        String cccdBackUrl = fileStorageService.storeImageFile(cccdBack, "cccd/" + ownerIdForPaths);
        String businessLicenseImageUrl = fileStorageService.storeImageFile(businessLicenseImage, "licenses/" + ownerIdForPaths);

        List<String> propertyImageUrls = propertyImages.stream()
                .map(file -> fileStorageService.storeImageFile(file, "properties/" + ownerIdForPaths))
                .collect(Collectors.toList());

        List<String> unitImageUrls = (unitImages != null && !unitImages.isEmpty())
                ? unitImages.stream()
                .map(file -> fileStorageService.storeImageFile(file, "units/" + ownerIdForPaths))
                .collect(Collectors.toList())
                : Collections.emptyList();

        OwnerApplicationData dataToStore = OwnerApplicationData.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .identityCardNumber(request.getIdentityCardNumber())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .city(request.getCity())
                .avatarUrl(avatarUrl)
                .cccdFrontUrl(cccdFrontUrl)
                .cccdBackUrl(cccdBackUrl)
                .propertyInfo(request.getPropertyInfo())
                .businessLicenseImage(businessLicenseImageUrl)
                .propertyImageUrls(propertyImageUrls)
                .unitImageUrls(unitImageUrls)
                .paymentInfo(request.getPaymentInfo())
                .build();

        String applicationDataJson;
        try {
            objectMapper.findAndRegisterModules();
            applicationDataJson = objectMapper.writeValueAsString(dataToStore);
        } catch (Exception e) {
            throw new InternalServerException("Failed to serialize application data: " + e.getMessage());
        }

        OwnerApplication application = ownerApplicationRepository.findByEmail(email)
                .orElse(OwnerApplication.builder().email(email).build());
        
        application.setApplicationData(applicationDataJson);
        application.setStatus(ApplicationStatus.PENDING);
        application.setRejectionReason(null);
        application.setReviewedAt(null);
        application.setReviewedBy(null);

        ownerApplicationRepository.save(application);

        emailService.sendHtmlEmail(email, "Đơn đăng ký đối tác đã được nhận", "email/application-submitted-confirmation", null);
    }
}

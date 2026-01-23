package com.example.smart_booking_system.dto.response.admin;

import com.example.smart_booking_system.enums.ApplicationStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
public class OwnerApplicationDTO {
    private Long id;
    private ApplicationStatus status;

    private String permanentAddress;
    private String hometownAddress;

    private String cardFrontImage;
    private String cardBackImage;
    private String businessLicenseImage;
    private String businessLicenseNumber;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String adminReason;


    private String applicantId;
    private String applicantFullName;
    private String applicantEmail;


    private String applicantPhoneNumber;
    private String applicantAvatar;
    private LocalDate applicantDob;
    private String personalIdCard;

    private String reviewedByAdminName;
}
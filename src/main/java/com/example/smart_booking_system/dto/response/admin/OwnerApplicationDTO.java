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
    private String gender;

    private String reviewedByAdminName;

    // Additional Property & Payment Info for Detailed View
    private PropertyInfoDTO propertyInfo;
    private PaymentInfoDTO paymentInfo;

    @Data
    public static class PropertyInfoDTO {
        private String propertyName;
        private String propertyType;
        private String description;
        private String propertyAddress;
        private String propertyCity;
        private String propertyDistrict;
        private String propertyWard;
        private double latitude;
        private double longitude;
        private String businessLicenseNumber;

        // Vẫn giữ lại area ở Property
        private double area;

        // Đã xóa price, weekendPrice, capacity ở đây

        private java.util.List<String> propertyImageUrls;
        private java.util.List<String> amenityNames;
        private PoliciesDTO policies;
        private UnitDataDTO unitData;
        private java.util.List<String> unitImageUrls;
    }

    @Data
    public static class PoliciesDTO {
        private String checkInTime;
        private String checkOutTime;
        private int minimumAge;
        private boolean allowFreeCancellation;
        private Integer freeCancellationDays;
    }

    @Data
    public static class PaymentInfoDTO {
        private String paymentMethod;
        private String bankName;
        private String accountHolderName;
        private String accountNumber;
    }

    @Data
    public static class UnitDataDTO {
        private String name;
        private String description;
        private Double area;

        // Đã chuyển các trường của phòng/căn xuống đây
        private Double price;
        private Double weekendPrice;
        private Integer capacity;

        private java.util.List<String> amenityNames;
    }
}
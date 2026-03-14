package com.example.smart_booking_system.dto.request;

import com.example.smart_booking_system.enums.PropertyType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// This DTO captures all the data submitted in the owner application form.
// It will be serialized to JSON and stored in the OwnerApplication entity.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerApplicationRequest {

    // User Profile Info
    private String fullName;
    private String phoneNumber;
    private String identityCardNumber;
    private String dateOfBirth; // Using String to keep it simple, will be parsed later
    private String gender;
    private String address;
    private String city;

    // Property Info
    private PropertyInfo propertyInfo;

    // Payment Info
    private PaymentInfo paymentInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyInfo {
        private PropertyType propertyType;
        private String propertyName;
        private String description;
        private String propertyAddress;
        private String propertyCity;
        private String propertyDistrict;
        private String propertyWard;
        private Double latitude;
        private Double longitude;
        private List<String> amenityIds;
        private String businessLicenseNumber;

        // Fields specific to VILLA/HOMESTAY
        private Double area; // Vẫn giữ lại area
        private UnitData unitData;

        private PolicyData policies;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitData {
        private String name;
        private String description;
        private Double area;

        // Đã chuyển xuống UnitData
        private BigDecimal price;
        private BigDecimal weekendPrice;
        private Integer capacity;

        // Simplified for storage, can be expanded later
        private List<String> amenityIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyData {
        private String checkInTime;
        private String checkOutTime;
        private Integer minimumAge;
        private boolean allowFreeCancellation;
        private Integer freeCancellationDays;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String paymentMethod;
        private String bankName;
        private String accountHolderName;
        private String accountNumber;

        private String stripeToken;
        private String cardLast4;
        private String cardBrand;
    }
}
package com.example.smart_booking_system.dto.request;

import com.example.smart_booking_system.enums.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Corresponds to the property-related fields in the JSON
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingPropertyInfoRequest {
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

    // --- Conditional data for Villa/Homestay ---
    private Double area; // Vẫn giữ lại area ở Property
    private OnboardingUnitDataRequest unitData; // nested object for unit details

    // --- Policies ---
    private PropertyPolicyRequest policies; // Reusing the policy DTO
}
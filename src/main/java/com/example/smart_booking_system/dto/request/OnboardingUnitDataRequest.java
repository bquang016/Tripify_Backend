package com.example.smart_booking_system.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

// Corresponds to the 'unitData' object for Villas/Homestays
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingUnitDataRequest {
    private String name;
    private String description;
    private Double area;
    private Map<String, Boolean> amenities;
}

package com.example.smart_booking_system.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

// Corresponds to the 'unitData' object for Villas/Homestays
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingUnitDataRequest {
    private String name;
    private String description;
    private Double area;

    // Đã thêm các thông tin dành cho phòng/căn
    private BigDecimal price;
    private BigDecimal weekendPrice;
    private Integer capacity;

    private Map<String, Boolean> amenities;
}
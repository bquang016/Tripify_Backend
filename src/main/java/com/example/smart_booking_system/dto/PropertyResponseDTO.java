package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.PropertyType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PropertyResponseDTO {

    private int propertyId;
    private String propertyName;
    private PropertyType propertyType;

    private String address;
    private String country;
    private String province;
    private String city;

    // ✅ [NEW]
    private String ward;
    private String provinceCode;
    private String districtCode;

    private String postalCode;
    private String description;
    private BigDecimal area;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phoneContact;
    private String emailContact;
    private BigDecimal rating;
    private int reviewCount;
    private boolean isActive;
    private PropertyStatus propertyStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String coverImage;
    private String ownerName;
    private List<String> images;
}
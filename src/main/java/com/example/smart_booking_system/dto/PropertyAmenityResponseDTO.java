package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.entity.PropertyAmenity; // ✅ Import Entity
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyAmenityResponseDTO {
    private int propertyAmenityId;
    private int propertyId;
    private int amenityId;
    private String amenityName;

    public PropertyAmenityResponseDTO(PropertyAmenity pa) {
        this.propertyAmenityId = pa.getPropertyAmenityId();
        if (pa.getProperty() != null) {
            this.propertyId = pa.getProperty().getPropertyId();
        }
        if (pa.getAmenity() != null) {
            this.amenityId = pa.getAmenity().getAmenityId();
            this.amenityName = pa.getAmenity().getAmenityName();
        }
    }
}
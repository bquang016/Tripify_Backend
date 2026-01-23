package com.example.smart_booking_system.dto.response.property;

import com.example.smart_booking_system.entity.Property;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PropertyMapDTO {
    private int propertyId;
    private String propertyName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal minPrice; // Để hiển thị "Từ 500k" trên bản đồ
    private BigDecimal rating;
    private int reviewCount;
    private String coverImage;   // Chỉ cần 1 ảnh bìa
    private String propertyType; // Để hiển thị icon tương ứng

    public PropertyMapDTO(Property property, String coverImageUrl, BigDecimal minPrice) {
        this.propertyId = property.getPropertyId();
        this.propertyName = property.getPropertyName();
        this.latitude = property.getLatitude();
        this.longitude = property.getLongitude();
        this.rating = property.getRating();
        this.reviewCount = property.getReviewCount();
        this.propertyType = property.getPropertyType().name();
        this.coverImage = coverImageUrl;
        this.minPrice = minPrice;
    }
}
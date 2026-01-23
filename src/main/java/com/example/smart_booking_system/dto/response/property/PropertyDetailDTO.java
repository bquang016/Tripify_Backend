package com.example.smart_booking_system.dto.response.property;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.PropertyType;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.PropertyAmenityResponseDTO;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import com.example.smart_booking_system.entity.PropertyImage;

@Data
@NoArgsConstructor
public class PropertyDetailDTO {
    private int propertyId;
    private String propertyName;
    private PropertyType propertyType;
    private String address;

    private String city;      // Quận/Huyện
    private String province;  // Tỉnh/Thành
    private String country;

    // ✅ [NEW] Thêm trường để hiển thị lại
    private String ward;
    private String provinceCode;
    private String districtCode;

    private String description;
    private BigDecimal rating;
    private int reviewCount;
    private PropertyStatus propertyStatus;
    private String coverImage;
    private String ownerId;
    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;
    private String ownerAvatar;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private BigDecimal latitude;  // Nên trả về lat/lng để hiển thị map
    private BigDecimal longitude;
    private LocalDate createdAt;
    private BigDecimal area;

    private List<String> images;
    private List<PropertyAmenityResponseDTO> amenities;
    private List<RoomResponseDTO> rooms;

    private boolean active;

    public PropertyDetailDTO(Property property) {
        this.propertyId = property.getPropertyId();
        this.propertyName = property.getPropertyName();
        this.propertyType = property.getPropertyType();
        this.address = property.getAddress();
        this.city = property.getCity();
        this.province = property.getProvince();
        this.country = property.getCountry();

        this.ward = property.getWard();
        this.provinceCode = property.getProvinceCode();
        this.districtCode = property.getDistrictCode();

        this.latitude = property.getLatitude();
        this.longitude = property.getLongitude();

        this.description = property.getDescription();
        this.rating = property.getRating();
        this.reviewCount = property.getReviewCount();
        this.propertyStatus = property.getPropertyStatus();
        this.createdAt = property.getCreatedAt();

        this.active = property.isActive();

        if (property.getOwner() != null) {
            this.ownerId = property.getOwner().getUserId();
            this.ownerName = property.getOwner().getFullName();
            this.ownerEmail = property.getOwner().getEmail();
            this.ownerPhone = property.getOwner().getPhoneNumber();
            if (property.getOwner().getUserDetail() != null) {
                this.ownerAvatar = property.getOwner().getUserDetail().getProfilePhotoUrl();
            } else {
                this.ownerAvatar = null;
            }
        }

        if (property.getImages() != null && !property.getImages().isEmpty()) {
            this.images = property.getImages().stream()
                    .map(PropertyImage::getImageUrl)
                    .collect(Collectors.toList());

            this.coverImage = property.getImages().stream()
                    .filter(PropertyImage::isCover)
                    .findFirst()
                    .map(PropertyImage::getImageUrl)
                    .orElse(this.images.get(0));
        }

        if (property.getPropertyDetail() != null) {
            this.area = property.getPropertyDetail().getArea();
        }
        }
    }
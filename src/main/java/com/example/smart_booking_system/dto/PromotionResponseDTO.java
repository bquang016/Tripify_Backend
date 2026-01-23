package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.DiscountType;
import com.example.smart_booking_system.enums.MembershipRank;
import com.example.smart_booking_system.enums.PromotionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PromotionResponseDTO {
    private Integer promotionId;
    private String code;
    private String description;
    private DiscountType discountType;
    private PromotionStatus status;
    private MembershipRank minMembershipRank;
    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal minBookingAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer usageCount;
    private String bannerUrl;


    private PropertyDTO property;

    public PromotionResponseDTO(Promotion p) {
        this.promotionId = p.getPromotionId();
        this.code = p.getCode();
        this.description = p.getDescription();
        this.discountType = p.getDiscountType();
        this.status = p.getStatus();
        this.minMembershipRank = p.getMinMembershipRank();
        this.discountValue = p.getDiscountValue();
        this.startDate = p.getStartDate();
        this.endDate = p.getEndDate();
        this.minBookingAmount = p.getMinBookingAmount();
        this.maxDiscountAmount = p.getMaxDiscountAmount();
        this.usageLimit = p.getUsageLimit();
        this.usageCount = p.getUsageCount();
        this.bannerUrl = p.getBannerUrl();


        if (p.getProperty() != null) {
            this.property = new PropertyDTO(p.getProperty());
        }
    }


    @Data
    @NoArgsConstructor
    public static class PropertyDTO {
        private Integer propertyId;
        private String propertyName;
        private String city;

        public PropertyDTO(Property property) {
            this.propertyId = property.getPropertyId();
            this.propertyName = property.getPropertyName();
            this.city = property.getCity();
        }
    }
}
package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.DiscountType;
import com.example.smart_booking_system.enums.PromotionStatus;
import com.example.smart_booking_system.enums.MembershipRank;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "propertyId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer promotionId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Enumerated(EnumType.STRING)
    private MembershipRank minMembershipRank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertyId")
    private Property property;

    @Column(nullable = false)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    private BigDecimal minBookingAmount;
    private BigDecimal maxDiscountAmount;

    private Integer usageLimit;
    private Integer usageCount = 0;

    @Column(length = 512)
    private String bannerUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (usageCount == null) usageCount = 0;
        if (status == null) {
            checkAndSetStatus();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void checkAndSetStatus() {
        if (this.status == PromotionStatus.DELETED || this.status == PromotionStatus.PAUSED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (endDate != null && endDate.isBefore(now)) {
            this.status = PromotionStatus.EXPIRED;
        } else {
            this.status = PromotionStatus.ACTIVE;
        }
    }
}
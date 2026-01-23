package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertyId")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomId")
    private Room room;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(length = 50)
    private String promotionCode;       // Lưu mã của OWNER

    @Column(length = 50)
    private String adminPromotionCode;  // Lưu mã của ADMIN

    @Column(precision = 15, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BookingStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    //Các trường tự nhập tay
    private String customerName;  // Tên người nhận phòng
    private String customerPhone; // SĐT liên hệ
    private String customerEmail; // Email nhận vé
    private String specialRequest; // Yêu cầu đặc biệt

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = BookingStatus.PENDING_PAYMENT;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

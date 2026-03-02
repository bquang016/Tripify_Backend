package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDTO {

    // ... (Giữ nguyên các trường cũ) ...
    private int bookingId;
    private int propertyId;
    private Integer roomId;
    private BigDecimal discountAmount;
    private String promotionCode; // Vẫn giữ trường này để tương thích logic cũ

    // --- [THÊM MỚI] 2 TRƯỜNG NÀY ---
    private String adminPromotionCode;
    private String ownerPromotionCode;
    // -------------------------------

    private String propertyName;
    private String propertyAddress;
    private String propertyImage;
    private String roomName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;
    private BigDecimal totalPrice;
    private BigDecimal penaltyAmount;
    private BigDecimal refundAmount;
    private BookingStatus status;
    private String paymentStatus;
    private String paymentMethod;
    private String specialRequest;
    private LocalDateTime createdAt;

    private String currency;
    private BigDecimal convertedTotalPrice;
    private BigDecimal convertedDiscountAmount;
    private BigDecimal convertedPenaltyAmount;
    private BigDecimal convertedRefundAmount;

    private UserSummaryDto user;
    private boolean isReviewed;

    // --- SỬA LẠI CONSTRUCTOR ---
    public BookingResponseDTO(Booking booking) {
        this.bookingId = booking.getBookingId();

        if (booking.getProperty() != null) {
            this.propertyId = booking.getProperty().getPropertyId();
            this.propertyName = booking.getProperty().getPropertyName();
            this.propertyAddress = booking.getProperty().getAddress();

            // Lưu ý: Kiểm tra lại tên getter bên Entity Property (getImages hay getPropertyImages)
            if (booking.getProperty().getImages() != null && !booking.getProperty().getImages().isEmpty()) {
                this.propertyImage = booking.getProperty().getImages().get(0).getImageUrl();
            }
        }

        if (booking.getRoom() != null) {
            this.roomId = booking.getRoom().getRoomId();
            this.roomName = booking.getRoom().getRoomName();
        }

        this.discountAmount = booking.getDiscountAmount();

        // --- [THÊM MỚI] Mapping rõ ràng 2 loại mã ---
        this.adminPromotionCode = booking.getAdminPromotionCode();
        this.ownerPromotionCode = booking.getPromotionCode();
        // -------------------------------------------

        // Logic cũ (giữ nguyên để không lỗi chỗ khác)
        this.promotionCode = booking.getPromotionCode();

        this.checkInDate = booking.getCheckInDate();
        this.checkOutDate = booking.getCheckOutDate();
        this.guestCount = booking.getGuestCount();
        this.totalPrice = booking.getTotalPrice();
        this.penaltyAmount = booking.getPenaltyAmount();
        this.refundAmount = booking.getRefundAmount();
        this.status = booking.getStatus();
        this.specialRequest = booking.getSpecialRequest();
        this.createdAt = booking.getCreatedAt();

        if (booking.getUser() != null) {
            this.user = new UserSummaryDto(
                    booking.getUser().getUserId(),
                    booking.getUser().getFullName(),
                    booking.getUser().getEmail(),
                    booking.getUser().getPhoneNumber()
            );
        } else {
            this.user = new UserSummaryDto(
                    null,
                    booking.getCustomerName(),
                    booking.getCustomerEmail(),
                    booking.getCustomerPhone()
            );
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryDto {
        private String userId;
        private String fullName;
        private String email;
        private String phoneNumber;
    }
}
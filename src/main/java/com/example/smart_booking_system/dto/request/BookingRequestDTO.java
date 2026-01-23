package com.example.smart_booking_system.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequestDTO {
    private String userId;
    private int propertyId;
    private Integer roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;

    // các trường tự nhập tay
    private String contactName;  // Tên người ở/liên hệ
    private String contactPhone; // SĐT người ở/liên hệ
    private String contactEmail; // Email người ở/liên hệ
    private String specialRequest;

    // Cờ đánh dấu xem có phải tự đặt cho mình không
    private boolean bookingForSelf;
}
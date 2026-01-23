package com.example.smart_booking_system.enums;

public enum BookingStatus {
    PENDING_PAYMENT,    // 1. Mới đặt, chờ thanh toán
    CONFIRMED,          // 2. Đã thanh toán/xác nhận (Sắp đến)
    CHECKED_IN,         // 3. Khách đã nhận phòng (Đang ở)
    COMPLETED,          // 4. Khách đã trả phòng (Hoàn tất)
    CANCELLED           // 5. Đã hủy (Khách hủy hoặc Admin hủy)
}
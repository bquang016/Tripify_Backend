package com.example.smart_booking_system.enums;

public enum PaymentStatus {
    PENDING,            // 0: Chờ thanh toán
    APPROVED,           // 1: Đã thanh toán (Tiền đã về Admin)
    REJECTED,           // 2: Thanh toán lỗi/thất bại
    REFUND_REQUESTED,   // 3: Khách đang yêu cầu hoàn tiền (Tiền bị treo)
    REFUNDED            // 4: Admin đã hoàn tiền xong
}
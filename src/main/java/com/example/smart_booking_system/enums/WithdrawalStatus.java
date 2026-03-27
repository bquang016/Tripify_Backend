package com.example.smart_booking_system.enums;

public enum WithdrawalStatus {
    PENDING,    // Chờ Admin duyệt
    APPROVED,   // Admin đã duyệt, đang gọi API Stripe
    COMPLETED,  // Stripe đã chuyển tiền thành công
    REJECTED,   // Admin từ chối
    FAILED      // Lỗi từ phía Stripe/Ngân hàng
}
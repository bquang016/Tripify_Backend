package com.example.smart_booking_system.enums;

public enum TransactionStatus {
    PENDING,   // Tiền đang bị giam (chờ hết hạn khiếu nại 48h)
    CLEARED,   // Tiền đã được cộng vào số dư khả dụng
    FAILED     // Giao dịch thất bại (nếu có lỗi)
}
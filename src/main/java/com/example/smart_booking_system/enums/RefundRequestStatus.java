package com.example.smart_booking_system.enums;

public enum RefundRequestStatus {
    PENDING,    // Đang chờ Admin duyệt
    APPROVED,   // Admin đã đồng ý (-> Payment sẽ thành REFUNDED)
    REJECTED    // Admin từ chối (-> Payment sẽ quay về APPROVED/PAID)
}
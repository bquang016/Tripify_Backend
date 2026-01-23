package com.example.smart_booking_system.enums;

public enum NotificationType {
    // --- NHÓM 1: CUSTOMER (Khách du lịch & Người dùng cá nhân) ---
    // Bao gồm cả các thông báo liên quan đến tài khoản và ứng tuyển
    GENERAL,            // Thông báo chung
    ACCOUNT_UPDATE,     // Cập nhật profile
    SECURITY_ALERT,     // Cảnh báo bảo mật

    BOOKING_SUCCESS,    // Đặt phòng thành công (Mình đi đặt)
    BOOKING_CANCELLED,  // Hủy phòng (Mình hủy)
    BOOKING_FAILED,     // Đặt thất bại
    PAYMENT_SUCCESS,    // Thanh toán thành công
    REFUND_PROCESSED,   // Hoàn tiền thành công
    PROMOTION,          // Khuyến mãi
    REMINDER_CHECKIN,   // Nhắc check-in

    // [QUAN TRỌNG] Kết quả duyệt đơn nằm ở đây để hiện trong Inbox cá nhân
    APPROVAL,           // Đơn đăng ký Owner/Hotel được duyệt
    REJECTION,          // Đơn đăng ký bị từ chối

    // --- NHÓM 2: OWNER (Dành cho công việc kinh doanh) ---
    BOOKING_RECEIVED,           // Có khách đặt phòng của tôi
    BOOKING_CANCELLED_BY_GUEST, // Khách hủy đặt phòng
    REVENUE_REPORT,             // Báo cáo doanh thu
    PROPERTY_SUSPENDED,         // Khách sạn bị tạm khóa
    ROOM_SUSPENDED,     // Thêm mới (dùng khi khóa phòng)
    SYSTEM,
    PAYOUT_SUCCESS,              // Thanh toán cho Owner thành công

    ADMIN_NEW_OWNER_REGISTRATION,   // Có đơn đăng ký Owner mới
    ADMIN_NEW_PROPERTY_SUBMISSION,  // Có khách sạn/căn hộ mới chờ duyệt
    ADMIN_NEW_REFUND_REQUEST,       // Có yêu cầu hoàn tiền mới
    ADMIN_SYSTEM_ALERT              // Cảnh báo hệ thống
}
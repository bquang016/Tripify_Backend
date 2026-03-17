package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.User;

public interface WalletService {
    // Hàm dùng để tạo ví rỗng cho Owner mới (Thường gọi khi Admin duyệt đơn đăng ký Owner)
    void createWallet(User owner);

    // Hàm xử lý cộng tiền vào Ví Pending khi Booking hoàn tất (Check-out)
    void addBookingRevenueToPending(Booking booking);
}
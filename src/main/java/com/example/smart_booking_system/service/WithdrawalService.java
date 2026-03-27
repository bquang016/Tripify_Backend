package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.WithdrawalRequest;
import java.math.BigDecimal;
import java.util.List;

public interface WithdrawalService {
    // Owner gọi để tạo yêu cầu rút tiền
    WithdrawalRequest requestWithdrawal(String ownerUserId, BigDecimal amount);

    // Admin gọi để duyệt yêu cầu rút tiền
    WithdrawalRequest approveWithdrawal(Long requestId, String adminUserId);

    // Admin gọi để từ chối yêu cầu rút tiền
    WithdrawalRequest rejectWithdrawal(Long requestId, String adminNote, String adminUserId);

    // Lấy lịch sử rút tiền của Owner
    List<WithdrawalRequest> getWithdrawalHistoryByOwner(String ownerUserId);

    // Lấy tất cả yêu cầu rút tiền (dành cho Admin)
    List<WithdrawalRequest> getAllWithdrawalRequests();
}
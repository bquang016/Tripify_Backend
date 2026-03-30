package com.example.smart_booking_system.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueReportDTO {
    private String period;          // Thời gian
    private String ownerName;       // Tên chủ cơ sở
    private String ownerEmail;      // Email chủ cơ sở
    private Integer totalBookings;  // Số lượng đơn thành công
    private Double grossRevenue;    // Tổng giao dịch (100%)
    private Double platformFee;     // Doanh thu sàn (15%)
    private Double ownerPayout;     // Chủ CS nhận (85%)
}
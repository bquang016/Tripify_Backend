package com.example.smart_booking_system.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAggregateDTO {
    private String period;         // Chu kỳ (VD: "15/03/2026", "03/2026", "2026")
    private Integer totalBookings; // Tổng số lượt đặt
    private Double totalRevenue;   // Tổng doanh thu
}
package com.example.smart_booking_system.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardStatsDTO {
    private BigDecimal totalRevenue;
    private long totalUsers;
    private long totalProperties; // Cần inject PropertyRepository để đếm
    private long newBookings24h;
    private List<MonthlyRevenueDTO> revenueChartData;

    @Data
    @AllArgsConstructor
    public static class MonthlyRevenueDTO {
        private String name; // Tên tháng (T1, T2...)
        private BigDecimal revenue;
    }
}
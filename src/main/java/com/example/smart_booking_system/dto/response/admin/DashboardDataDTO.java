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
public class DashboardDataDTO {
    // 1. Thẻ thống kê (Stat Cards)
    private BigDecimal totalRevenue;
    private long totalUsers;        // ✅ Sửa lỗi: AdminController cần trường này
    private long totalProperties;
    private long newBookings24h;

    // 2. Dữ liệu biểu đồ (Charts)
    private List<ChartData> revenueChart;      // Doanh thu theo tháng
    private List<ChartData> bookingTrends;     // Số lượng đơn theo tháng
    private List<ChartData> userGrowth;        // User mới theo tháng
    private List<PieChartData> revenueByType;  // Cơ cấu doanh thu (Pie chart)

    // 3. Danh sách (Lists/Tables)
    private List<TopHotelDTO> topHotels;       // ✅ Sửa lỗi: AdminController cần class này
    private List<RecentBookingDTO> recentBookings;

    // --- Inner Classes cho cấu trúc con ---
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChartData {
        private String name; // Ví dụ: "T1", "T2"
        private Number value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PieChartData {
        private String name;  // Ví dụ: "HOTEL", "VILLA"
        private Number value;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopHotelDTO { // ✅ Class con mà AdminController đang tìm kiếm
        private String name;
        private BigDecimal revenue;
        private long bookings;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecentBookingDTO {
        private int id;
        private String customerName;
        private String propertyName;
        private BigDecimal price;
        private String status;
        private String date;
    }
}
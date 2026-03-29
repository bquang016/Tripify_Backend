package com.example.smart_booking_system.dto.response.owner;

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
public class OwnerDashboardDTO {
    // --- Stat Cards ---
    private long checkInToday;
    private long checkOutToday;
    private BigDecimal revenueToday; // Doanh thu bookings tạo hôm nay
    private BigDecimal totalRevenue; // Tổng doanh thu toàn thời gian
    private long totalProperties;
    private long newBookings24h;

    // --- Charts ---
    private List<ChartData> revenueChart;      // Biểu đồ doanh thu năm
    private List<ChartData> bookingTrends;     // Biểu đồ xu hướng đặt phòng
    private List<PieChartData> revenueByType;  // Cơ cấu doanh thu

    // --- Widgets ---
    private List<ReviewDTO> recentReviews;     // Đánh giá gần đây
    private List<RecentBookingDTO> recentBookings; // Booking gần đây

    // --- Inner Classes ---
    @Data
    @AllArgsConstructor
    public static class ChartData {
        private String name;
        private Number value;
    }

    @Data
    @AllArgsConstructor
    public static class PieChartData {
        private String name;
        private Number value;
        private Number count;
    }

    @Data
    @Builder
    public static class ReviewDTO {
        private Integer id;
        private String user;
        private Integer rating;
        private String text;
        private String date;
        private String reply;
    }

    @Data
    @Builder
    public static class RecentBookingDTO {
        private int id;
        private String customerName;
        private String propertyName;
        private BigDecimal price;
        private String status;
        private String date;
    }
}
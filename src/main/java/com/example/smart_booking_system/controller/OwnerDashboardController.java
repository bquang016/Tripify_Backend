package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.owner.OwnerDashboardDTO;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Rating;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.repository.RatingRepository;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/owner/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerDashboardController {

    private final BookingRepository bookingRepo;
    private final PropertyRepository propertyRepo;
    private final UserRepository userRepo;
    private final RatingRepository ratingRepo;

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(Authentication authentication) {
        try {
            // 1. Lấy thông tin Owner đang đăng nhập
            String username = authentication.getName();
            User owner = userRepo.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin chủ sở hữu"));
            String ownerId = owner.getUserId();

            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();

            // 2. Số liệu thống kê cơ bản (Stat Cards)
            long checkInToday = bookingRepo.countCheckInsByOwner(ownerId, today);
            long checkOutToday = bookingRepo.countCheckOutsByOwner(ownerId, today);
            BigDecimal revenueToday = bookingRepo.calculateRevenueTodayByOwner(ownerId, today);
            BigDecimal totalRevenue = bookingRepo.calculateTotalRevenueByOwner(ownerId);
            long totalProperties = propertyRepo.countByOwner_UserId(ownerId);
            long newBookings24h = bookingRepo.countNewBookingsByOwner(ownerId, LocalDateTime.now().minusDays(1));

            // 3. Biểu đồ (Charts)
            List<OwnerDashboardDTO.ChartData> revenueChart = processChartData(bookingRepo.getMonthlyRevenueByOwner(ownerId, currentYear));
            List<OwnerDashboardDTO.ChartData> bookingTrends = processChartData(bookingRepo.getMonthlyBookingCountByOwner(ownerId, currentYear));

            // 4. Cơ cấu doanh thu (Pie Chart)
            List<Object[]> typeData = bookingRepo.getRevenueByPropertyTypeByOwner(ownerId);
            List<OwnerDashboardDTO.PieChartData> revenueByType = new ArrayList<>();
            if (typeData != null) {
                for (Object[] obj : typeData) {
                    revenueByType.add(new OwnerDashboardDTO.PieChartData(obj[0].toString(), (Number) obj[1]));
                }
            }

            // 5. Đánh giá gần đây (Review Widget)
            List<Rating> reviewsRaw = ratingRepo.findRecentReviewsByOwner(ownerId, PageRequest.of(0, 3));
            List<OwnerDashboardDTO.ReviewDTO> recentReviews = reviewsRaw.stream()
                    .map(r -> OwnerDashboardDTO.ReviewDTO.builder()
                            .id(r.getRatingId())
                            .user(r.getUserId() != null ? r.getUserId().getFullName() : "Ẩn danh")
                            .rating(r.getRating())
                            .text(r.getComment())
                            // ✅ FIX LỖI 500 Ở ĐÂY: Kiểm tra null cho createdAt
                            .date(r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : "")
                            .build())
                    .toList();

            // 6. Booking gần đây (Table)
            List<Booking> recentRaw = bookingRepo.findRecentBookingsByOwner(ownerId);
            List<OwnerDashboardDTO.RecentBookingDTO> recentBookings = recentRaw.stream()
                    .map(b -> OwnerDashboardDTO.RecentBookingDTO.builder()
                            .id(b.getBookingId())
                            .customerName(b.getCustomerName() != null ? b.getCustomerName() : b.getUser().getFullName())
                            .propertyName(b.getProperty().getPropertyName())
                            .price(b.getTotalPrice())
                            .status(b.getStatus().name())
                            // Kiểm tra null cho createdAt của booking (đề phòng)
                            .date(b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate().toString() : "")
                            .build())
                    .toList();

            // 7. Đóng gói dữ liệu
            OwnerDashboardDTO response = OwnerDashboardDTO.builder()
                    .checkInToday(checkInToday)
                    .checkOutToday(checkOutToday)
                    .revenueToday(revenueToday != null ? revenueToday : BigDecimal.ZERO)
                    .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                    .totalProperties(totalProperties)
                    .newBookings24h(newBookings24h)
                    .revenueChart(revenueChart)
                    .bookingTrends(bookingTrends)
                    .revenueByType(revenueByType)
                    .recentReviews(recentReviews)
                    .recentBookings(recentBookings)
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Lấy thống kê Owner Dashboard thành công", response));

        } catch (Exception e) {
            e.printStackTrace(); // Log lỗi ra console server để debug
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi Server: " + e.getMessage()));
        }
    }

    // Helper: Điền đủ 12 tháng cho biểu đồ
    private List<OwnerDashboardDTO.ChartData> processChartData(List<Object[]> rawData) {
        Map<Integer, Number> dataMap = new HashMap<>();
        if (rawData != null) {
            for (Object[] row : rawData) {
                dataMap.put((Integer) row[0], (Number) row[1]);
            }
        }
        List<OwnerDashboardDTO.ChartData> result = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            result.add(new OwnerDashboardDTO.ChartData("T" + i, dataMap.getOrDefault(i, 0)));
        }
        return result;
    }
}
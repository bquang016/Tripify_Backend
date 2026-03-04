package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.admin.RejectReasonDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.admin.DashboardDataDTO; // ✅ Đổi sang DTO mới đầy đủ hơn
import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.enums.ApplicationStatus;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.service.OwnerApplicationService;
import com.example.smart_booking_system.security.CheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest; // ✅ Import thêm
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap; // ✅ Import thêm
import java.util.List;
import java.util.Map;     // ✅ Import thêm

@RestController
@RequestMapping("/api/v1/admin/owner-applications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@CheckPermission("APPLICATION_MANAGE")
public class AdminApplicationController {

    private final OwnerApplicationService ownerApplicationService;
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final PropertyRepository propertyRepo;

    @GetMapping
    public ResponseEntity<?> getOwnerApplications(@RequestParam(required = false) String status) {
        try {
            String statusStr = (status != null && !status.isEmpty()) ? status : "PENDING";
            ApplicationStatus appStatus = ApplicationStatus.valueOf(statusStr.toUpperCase());
            List<OwnerApplicationDTO> applications = ownerApplicationService.getApplicationsByStatus(appStatus);

            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách đơn " + appStatus + " thành công",
                    applications
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái không hợp lệ: " + status));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<OwnerApplicationDTO>> getApplicationDetail(@PathVariable Long applicationId) {
        OwnerApplicationDTO application = ownerApplicationService.getApplicationDetail(applicationId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết đơn đăng ký thành công", application));
    }

    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveApplication(
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        String adminUsername = authentication.getName();
        ownerApplicationService.approveApplication(applicationId, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Đã duyệt đơn đăng ký thành công.", null));
    }

    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody RejectReasonDTO rejectReason,
            Authentication authentication
    ) {
        String adminUsername = authentication.getName();
        ownerApplicationService.rejectApplication(applicationId, rejectReason.getReason(), adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối đơn đăng ký.", null));
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String ownerId
    ) {
        try {
            int queryYear = (year != null) ? year : LocalDate.now().getYear();
            Integer queryMonth = month; // null = cả năm
            String queryCity = (city != null && !city.trim().isEmpty()) ? city.trim() : null;
            String queryOwnerId = (ownerId != null && !ownerId.trim().isEmpty()) ? ownerId.trim() : null;

            BigDecimal revenue = bookingRepo.calculateFilteredRevenue(queryYear, queryMonth, queryCity, queryOwnerId);
            long users = userRepo.count();
            long properties = propertyRepo.count();
            long newBookings = bookingRepo.countFilteredBookings(queryYear, queryMonth, queryCity, queryOwnerId);

            List<DashboardDataDTO.ChartData> revenueChart =
                    processChartData(bookingRepo.getFilteredMonthlyRevenue(queryYear, queryCity, queryOwnerId));

            List<DashboardDataDTO.ChartData> bookingTrends =
                    processChartData(bookingRepo.getFilteredMonthlyBookingCount(queryYear, queryCity, queryOwnerId));

            List<DashboardDataDTO.ChartData> userGrowth =
                    processChartData(userRepo.getMonthlyUserGrowth(queryYear));

            List<Object[]> typeData =
                    bookingRepo.getFilteredRevenueByPropertyType(queryYear, queryMonth, queryCity, queryOwnerId);

            List<DashboardDataDTO.PieChartData> revenueByType = new ArrayList<>();
            if (typeData != null) {
                for (Object[] obj : typeData) {
                    revenueByType.add(new DashboardDataDTO.PieChartData(
                            obj[0].toString(),
                            (Number) obj[1]
                    ));
                }
            }

            List<Object[]> topHotelsRaw = bookingRepo.getTopPerformingHotels(PageRequest.of(0, 5));
            List<DashboardDataDTO.TopHotelDTO> topHotels = new ArrayList<>();
            if (topHotelsRaw != null) {
                for (Object[] obj : topHotelsRaw) {
                    topHotels.add(DashboardDataDTO.TopHotelDTO.builder()
                            .name((String) obj[0])
                            .revenue((BigDecimal) obj[1])
                            .bookings((Long) obj[2])
                            .build());
                }
            }

            List<Booking> recentRaw = bookingRepo.findTop10ByOrderByCreatedAtDesc();
            List<DashboardDataDTO.RecentBookingDTO> recentBookings = recentRaw.stream()
                    .map(b -> DashboardDataDTO.RecentBookingDTO.builder()
                            .id(b.getBookingId())
                            .customerName(b.getCustomerName() != null ? b.getCustomerName() : b.getUser().getFullName())
                            .propertyName(b.getProperty().getPropertyName())
                            .price(b.getTotalPrice())
                            .status(b.getStatus().name())
                            .date(b.getCreatedAt().toLocalDate().toString())
                            .build())
                    .toList();

            DashboardDataDTO stats = DashboardDataDTO.builder()
                    .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                    .totalUsers(users)
                    .totalProperties(properties)
                    .newBookings24h(newBookings)
                    .revenueChart(revenueChart)
                    .bookingTrends(bookingTrends)
                    .userGrowth(userGrowth)
                    .revenueByType(revenueByType)
                    .topHotels(topHotels)
                    .recentBookings(recentBookings)
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Lấy thống kê Dashboard thành công", stats));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi Server: " + e.getMessage()));
        }
    }
    
    @GetMapping("/dashboard/owners")
    public ResponseEntity<?> getOwnersForFilter() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Danh sách chủ sở hữu",
                        userRepo.findOwnersForDashboard()
                )
        );
    }

    @GetMapping("/dashboard/cities")
    public ResponseEntity<?> getCitiesForFilter() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Danh sách khu vực",
                        bookingRepo.findAllCitiesForDashboard()
                )
        );
    }

    private List<DashboardDataDTO.ChartData> processChartData(List<Object[]> rawData) {
        Map<Integer, Number> dataMap = new HashMap<>();
        if (rawData != null) {
            for (Object[] row : rawData) {
                dataMap.put((Integer) row[0], (Number) row[1]);
            }
        }

        List<DashboardDataDTO.ChartData> result = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            result.add(new DashboardDataDTO.ChartData(
                    "T" + i,
                    dataMap.getOrDefault(i, 0)
            ));
        }
        return result;
    }
}
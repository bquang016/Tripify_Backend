package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.report.RevenueReportDTO;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final BookingRepository bookingRepository;

    @GetMapping("/owner/revenue")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<byte[]> downloadOwnerRevenue(
            @RequestParam String format, // "pdf" hoặc "excel"
            @RequestParam String startDate,
            @RequestParam String endDate,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        try {
            // 1. Parse thời gian
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // 2. Lấy dữ liệu thật từ Database
            List<Booking> bookings = bookingRepository.findBookingsForRevenueReport(currentUser.getUserId(), start, end);

            // 3. Gom nhóm dữ liệu theo Tháng/Năm (VD: "01/2026") bằng Java Stream
            DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

            Map<YearMonth, List<Booking>> groupedBookings = bookings.stream()
                    .collect(Collectors.groupingBy(b -> YearMonth.from(b.getCheckInDate())));

            // 4. Tính tổng doanh thu và lượt đặt cho từng tháng (Dữ liệu cho Bảng chi tiết)
            List<RevenueReportDTO> reportData = groupedBookings.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()) // Sắp xếp theo thời gian tăng dần
                    .map(entry -> {
                        YearMonth ym = entry.getKey();
                        List<Booking> monthBookings = entry.getValue();

                        int totalBookings = monthBookings.size();
                        double totalRevenue = monthBookings.stream()
                                .map(Booking::getTotalPrice)
                                .mapToDouble(BigDecimal::doubleValue)
                                .sum();

                        return new RevenueReportDTO(ym.format(monthYearFormatter), totalBookings, totalRevenue);
                    })
                    .collect(Collectors.toList());

            // Xử lý trường hợp không có dữ liệu trong khoảng thời gian đó
            if (reportData.isEmpty()) {
                reportData.add(new RevenueReportDTO("Không có dữ liệu", 0, 0.0));
            }

            // 5. Tính toán các con số TÓM TẮT (Theo đặc tả B6)
            LocalDate today = LocalDate.now();

            // Số đơn mới hôm nay
            long newOrdersToday = bookings.stream()
                    .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().isEqual(today))
                    .count();

            // Doanh thu phát sinh trong ngày
            double todayRevenue = bookings.stream()
                    .filter(b -> b.getCheckInDate() != null && b.getCheckInDate().isEqual(today))
                    .map(Booking::getTotalPrice)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();

            // Tổng doanh thu tích lũy (Trong khoảng thời gian báo cáo)
            double totalCumulativeRevenue = reportData.stream()
                    .filter(r -> !r.getMonth().equals("Không có dữ liệu"))
                    .mapToDouble(RevenueReportDTO::getTotalRevenue)
                    .sum();

            // 6. Truyền tham số cho Header báo cáo
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("ownerName", currentUser.getFullName());
            // Đổi định dạng hiển thị cho đẹp
            parameters.put("reportDate", start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " đến " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            // Truyền các thông số đặc tả B6
            parameters.put("totalCumulativeRevenue", totalCumulativeRevenue);
            parameters.put("todayRevenue", todayRevenue);
            parameters.put("newOrdersToday", (int) newOrdersToday);

            // 7. Gọi service tạo file
            byte[] reportBytes = reportService.generateReport("owner_revenue", parameters, reportData, format);

            // 8. Thiết lập HttpHeaders để trình duyệt hiểu đây là file tải về
            HttpHeaders headers = new HttpHeaders();
            String extension = format.equalsIgnoreCase("excel") ? "xlsx" : "pdf";
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Doanh_Thu_" + currentUser.getUserId() + "." + extension);

            MediaType mediaType = format.equalsIgnoreCase("excel")
                    ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    : MediaType.APPLICATION_PDF;

            headers.setContentType(mediaType);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
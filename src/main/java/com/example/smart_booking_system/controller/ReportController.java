package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.report.AdminRevenueReportDTO;
import com.example.smart_booking_system.dto.response.report.BookingDetailReportDTO;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;

    @GetMapping("/owner/revenue")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<byte[]> downloadOwnerRevenue(
            @RequestParam String format,
            @RequestParam String reportType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "ALL") String propertyId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // 1. Xử lý PropertyId và lấy tên cơ sở lưu trú
            Integer parsedPropertyId = null;
            String propertyNameStr = "Tất cả cơ sở lưu trú";

            if (!"ALL".equalsIgnoreCase(propertyId)) {
                parsedPropertyId = Integer.parseInt(propertyId);
                propertyNameStr = propertyRepository.findById(parsedPropertyId)
                        .map(com.example.smart_booking_system.entity.Property::getPropertyName)
                        .orElse("Không xác định");
            }

            // 2. Lấy dữ liệu Booking
            List<Booking> bookings = bookingRepository.findBookingsForRevenueReportWithProperty(
                    currentUser.getUserId(), parsedPropertyId, start, end);

            List<BookingDetailReportDTO> reportData = new ArrayList<>();
            String templateName = "owner_revenue_detailed";
            String reportTitle = "";

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // 3. Map dữ liệu sang DTO chi tiết
            for (Booking b : bookings) {
                String period = "";
                if ("DAILY".equalsIgnoreCase(reportType)) {
                    reportTitle = "BÁO CÁO DOANH THU THEO NGÀY";
                    period = b.getCheckInDate() != null ? b.getCheckInDate().format(dateFmt) : "";
                } else if ("MONTHLY".equalsIgnoreCase(reportType)) {
                    reportTitle = "BÁO CÁO DOANH THU THEO THÁNG";
                    period = b.getCheckInDate() != null ? YearMonth.from(b.getCheckInDate()).format(DateTimeFormatter.ofPattern("MM/yyyy")) : "";
                } else if ("YEARLY".equalsIgnoreCase(reportType)) {
                    reportTitle = "BÁO CÁO DOANH THU THEO NĂM";
                    period = b.getCheckInDate() != null ? String.valueOf(b.getCheckInDate().getYear()) : "";
                }

                // Cập nhật lấy theo trường khách nhập tay trong entity Booking của bạn
                String custName = b.getCustomerName() != null ? b.getCustomerName() :
                        (b.getUser() != null ? b.getUser().getFullName() : "Khách lẻ");

                String email = b.getCustomerEmail() != null ? b.getCustomerEmail() :
                        (b.getUser() != null ? b.getUser().getEmail() : "Không có");

                String created = b.getCreatedAt() != null ? b.getCreatedAt().format(timeFmt) : "";
                String checkIn = b.getCheckInDate() != null ? b.getCheckInDate().format(dateFmt) : "";
                String status = b.getStatus() != null ? b.getStatus().name() : "";

                // Lấy tên Cơ sở lưu trú của từng Booking
                String bookingPropertyName = (b.getProperty() != null && b.getProperty().getPropertyName() != null)
                        ? b.getProperty().getPropertyName() : "Không xác định";

                Double price = b.getTotalPrice() != null ? b.getTotalPrice().doubleValue() : 0.0;

                // Đưa vào DTO 8 tham số
                reportData.add(new BookingDetailReportDTO(period, custName, email, created, checkIn, status, bookingPropertyName, price));
            }

            if (reportData.isEmpty()) {
                reportData.add(new BookingDetailReportDTO("N/A", "Không có dữ liệu", "-", "-", "-", "-", "-", 0.0));
            }

            // 4. Truyền parameters cho Header (Thêm propertyName)
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("systemName", "TRIPIFY PARTNER");
            parameters.put("reportTitle", reportTitle);
            parameters.put("ownerName", currentUser.getFullName());
            parameters.put("propertyName", propertyNameStr);
            parameters.put("reportPeriod", "Từ ngày " + start.format(dateFmt) + " đến " + end.format(dateFmt));
            parameters.put("reportId", "REP-" + System.currentTimeMillis());
            parameters.put("generationTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            // 5. Sinh file báo cáo
            byte[] reportBytes = reportService.generateReport(templateName, parameters, reportData, format);

            HttpHeaders headers = new HttpHeaders();
            String extension = format.equalsIgnoreCase("excel") ? "xlsx" : "pdf";
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bao_Cao_" + reportType + "_" + currentUser.getUserId() + "." + extension);

            MediaType mediaType = format.equalsIgnoreCase("excel")
                    ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    : MediaType.APPLICATION_PDF;
            headers.setContentType(mediaType);

            return ResponseEntity.ok().headers(headers).body(reportBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/admin/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadAdminRevenue(
            @RequestParam String format,
            @RequestParam String reportType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // ✅ Thay đổi: Kéo dữ liệu đã được lọc trực tiếp từ Database thay vì findAll()
            List<Booking> bookings = bookingRepository.findAdminBookingsForRevenueReport(start, end);

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String reportTitle = "BÁO CÁO DOANH THU SÀN THEO " +
                    (reportType.equals("DAILY") ? "NGÀY" : reportType.equals("MONTHLY") ? "THÁNG" : "NĂM");

            // Gom nhóm Booking theo: Chu kỳ thời gian + Owner ID
            Map<String, List<Booking>> groupedBookings = bookings.stream().collect(Collectors.groupingBy(b -> {
                String periodStr = "";
                if ("DAILY".equalsIgnoreCase(reportType)) {
                    periodStr = b.getCheckInDate().format(dateFmt);
                } else if ("MONTHLY".equalsIgnoreCase(reportType)) {
                    periodStr = YearMonth.from(b.getCheckInDate()).format(DateTimeFormatter.ofPattern("MM/yyyy"));
                } else {
                    periodStr = String.valueOf(b.getCheckInDate().getYear());
                }
                String ownerId = b.getProperty().getOwner().getUserId();
                return periodStr + "|" + ownerId;
            }));

            List<AdminRevenueReportDTO> reportData = new ArrayList<>();

            // Tính toán tổng số cho từng nhóm
            for (Map.Entry<String, List<Booking>> entry : groupedBookings.entrySet()) {
                String[] keys = entry.getKey().split("\\|");
                String period = keys[0];

                List<Booking> group = entry.getValue();
                Booking firstBooking = group.get(0);

                String ownerName = firstBooking.getProperty().getOwner().getFullName();
                String ownerEmail = firstBooking.getProperty().getOwner().getEmail();

                int totalBookings = group.size();
                double grossRevenue = group.stream().mapToDouble(b -> b.getTotalPrice().doubleValue()).sum();
                double platformFee = grossRevenue * 0.15; // Phí sàn 15%
                double ownerPayout = grossRevenue * 0.85; // Tiền trả cho chủ CS 85%

                reportData.add(new AdminRevenueReportDTO(period, ownerName, ownerEmail, totalBookings, grossRevenue, platformFee, ownerPayout));
            }

            // Sắp xếp báo cáo theo thời gian
            reportData.sort(Comparator.comparing(AdminRevenueReportDTO::getPeriod));

            if (reportData.isEmpty()) {
                reportData.add(new AdminRevenueReportDTO("N/A", "Không có dữ liệu", "-", 0, 0.0, 0.0, 0.0));
            }

            // Truyền tham số cho Header (Đã khớp với file admin_revenue_detailed.jrxml)
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("systemName", "Tripify Admin Portal");
            parameters.put("reportTitle", reportTitle);
            parameters.put("reportPeriod", "Từ ngày " + start.format(dateFmt) + " đến " + end.format(dateFmt));
            parameters.put("reportId", "ADM-REV-" + System.currentTimeMillis());
            parameters.put("generationTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            parameters.put("generatedBy", currentUser.getFullName());

            byte[] reportBytes = reportService.generateReport("admin_revenue_detailed", parameters, reportData, format);

            HttpHeaders headers = new HttpHeaders();
            String extension = format.equalsIgnoreCase("excel") ? "xlsx" : "pdf";
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Admin_DoanhThu_" + reportType + "." + extension);
            headers.setContentType(format.equalsIgnoreCase("excel") ?
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") : MediaType.APPLICATION_PDF);

            return ResponseEntity.ok().headers(headers).body(reportBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
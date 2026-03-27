package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.report.RevenueAggregateDTO;
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
            LocalDate today = LocalDate.now();

            // ✅ XỬ LÝ CHUYỂN "ALL" THÀNH NULL
            Integer parsedPropertyId = null;
            if (!"ALL".equalsIgnoreCase(propertyId)) {
                parsedPropertyId = Integer.parseInt(propertyId);
            }

            // SỬ DỤNG HÀM MỚI Ở REPOSITORY
            List<Booking> bookings = bookingRepository.findBookingsForRevenueReportWithProperty(
                    currentUser.getUserId(), parsedPropertyId, start, end);

            // 2. Gom nhóm dữ liệu dựa theo reportType
            List<RevenueAggregateDTO> reportData = new ArrayList<>();
            String templateName = "";

            if ("DAILY".equalsIgnoreCase(reportType)) {
                templateName = "owner_revenue_daily";
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                Map<LocalDate, List<Booking>> dailyMap = bookings.stream()
                        .filter(b -> b.getCheckInDate() != null)
                        .collect(Collectors.groupingBy(Booking::getCheckInDate));

                reportData = dailyMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .map(e -> new RevenueAggregateDTO(
                                e.getKey().format(fmt),
                                e.getValue().size(),
                                e.getValue().stream().mapToDouble(b -> b.getTotalPrice().doubleValue()).sum()))
                        .collect(Collectors.toList());

            } else if ("MONTHLY".equalsIgnoreCase(reportType)) {
                templateName = "owner_revenue_monthly";
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");

                Map<YearMonth, List<Booking>> monthlyMap = bookings.stream()
                        .filter(b -> b.getCheckInDate() != null)
                        .collect(Collectors.groupingBy(b -> YearMonth.from(b.getCheckInDate())));

                reportData = monthlyMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .map(e -> new RevenueAggregateDTO(
                                e.getKey().format(fmt),
                                e.getValue().size(),
                                e.getValue().stream().mapToDouble(b -> b.getTotalPrice().doubleValue()).sum()))
                        .collect(Collectors.toList());

            } else if ("YEARLY".equalsIgnoreCase(reportType)) {
                templateName = "owner_revenue_yearly";

                Map<Integer, List<Booking>> yearlyMap = bookings.stream()
                        .filter(b -> b.getCheckInDate() != null)
                        .collect(Collectors.groupingBy(b -> b.getCheckInDate().getYear()));

                reportData = yearlyMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .map(e -> new RevenueAggregateDTO(
                                String.valueOf(e.getKey()),
                                e.getValue().size(),
                                e.getValue().stream().mapToDouble(b -> b.getTotalPrice().doubleValue()).sum()))
                        .collect(Collectors.toList());
            }

            if (reportData.isEmpty()) {
                reportData.add(new RevenueAggregateDTO("Không có dữ liệu", 0, 0.0));
            }

            // 3. Tính toán Snapshot Tóm tắt
            long newOrdersToday = bookings.stream().filter(b -> b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().isEqual(today)).count();
            double todayRevenue = bookings.stream().filter(b -> b.getCheckInDate() != null && b.getCheckInDate().isEqual(today)).mapToDouble(b -> b.getTotalPrice().doubleValue()).sum();
            double totalCumulativeRevenue = reportData.stream().filter(r -> !r.getPeriod().equals("Không có dữ liệu")).mapToDouble(RevenueAggregateDTO::getTotalRevenue).sum();

            // 4. Đưa vào Parameters cho File Report
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("ownerName", currentUser.getFullName());
            parameters.put("reportDate", start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " đến " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            parameters.put("generationTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            parameters.put("totalCumulativeRevenue", totalCumulativeRevenue);
            parameters.put("todayRevenue", todayRevenue);
            parameters.put("newOrdersToday", (int) newOrdersToday);

            // 5. Xuất file
            byte[] reportBytes = reportService.generateReport(templateName, parameters, reportData, format);

            HttpHeaders headers = new HttpHeaders();
            String extension = format.equalsIgnoreCase("excel") ? "xlsx" : "pdf";
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Doanh_Thu_" + reportType + "_" + currentUser.getUserId() + "." + extension);

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
}
package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PropertyDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/internal/ai-tools")
@RequiredArgsConstructor
public class InternalAiToolController {

    private final PropertyDetailRepository propertyDetailRepository;

    // ✅ Thêm BookingRepository để gọi hàm check phòng
    private final BookingRepository bookingRepository;

    @Value("${ai.internal.secret}")
    private String internalSecret;

    @GetMapping("/search-hotels")
    public ResponseEntity<?> searchHotels(
            @RequestHeader("X-Internal-Secret") String secretHeader,
            @RequestParam String city,
            @RequestParam int capacity) {

        // 1. Kiểm tra khóa bảo mật từ n8n
        if (!internalSecret.equals(secretHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Sai khóa bảo mật nội bộ!");
        }

        // 2. Lấy dữ liệu từ Database
        List<Property> properties = propertyDetailRepository.findAvailableProperties(city.toLowerCase(), capacity);

        // 3. TỐI ƯU DTO CHO AI
        List<Map<String, Object>> optimizedResults = properties.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("propertyId", p.getPropertyId());
            map.put("name", p.getPropertyName());
            map.put("address", p.getAddress() != null ? p.getAddress() : "Đang cập nhật");
            map.put("rating", 5);
            map.put("bookingUrl", "/hotels/" + p.getPropertyId());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(optimizedResults);
    }

    // ✅ API MỚI: Dành cho n8n check phòng trống
    @GetMapping("/check-availability")
    public ResponseEntity<?> checkRoomAvailability(
            @RequestHeader("X-Internal-Secret") String secretHeader,
            @RequestParam Integer roomId,
            @RequestParam String checkInDate, // n8n gửi dạng "YYYY-MM-DD"
            @RequestParam String checkOutDate) {

        // 1. Validate secret key
        if (!internalSecret.equals(secretHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
        }

        try {
            // 2. Convert String từ n8n thành LocalDate của Java
            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);

            // Logic cơ bản: Check out không thể trước check in
            if (!checkOut.isAfter(checkIn)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "isAvailable", false,
                        "message", "Lỗi: Ngày trả phòng phải sau ngày nhận phòng."));
            }

            // 3. Gọi hàm đã có sẵn trong BookingRepository
            Long overlappingCount = bookingRepository.countExistingBookings(roomId, checkIn, checkOut);

            // 4. Đóng gói kết quả cho AI
            boolean isAvailable = (overlappingCount == null || overlappingCount == 0);

            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            response.put("isAvailable", isAvailable);
            response.put("message", isAvailable ? "Phòng đang trống, bạn có thể hướng dẫn khách đặt phòng."
                    : "Phòng đã có người đặt trong giai đoạn này, hãy gợi ý ngày khác hoặc phòng khác.");

            return ResponseEntity.ok(response);

        } catch (DateTimeParseException e) {
            // Xử lý n8n truyền sai format ngày
            return ResponseEntity.badRequest().body(Map.of(
                    "isAvailable", false,
                    "error", "Định dạng ngày không hợp lệ. AI Agent vui lòng truyền định dạng YYYY-MM-DD."));
        }
    }
}
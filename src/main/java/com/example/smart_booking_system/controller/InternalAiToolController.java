package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.repository.PropertyDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/internal/ai-tools")
@RequiredArgsConstructor
public class InternalAiToolController {

    private final PropertyDetailRepository propertyDetailRepository;

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

        // 3. TỐI ƯU DTO CHO AI (Mớm đủ dữ liệu để AI nặn ra JSON chuẩn cho Frontend)
        List<Map<String, Object>> optimizedResults = properties.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("propertyId", p.getPropertyId());
            map.put("name", p.getPropertyName());
            map.put("address", p.getAddress() != null ? p.getAddress() : "Đang cập nhật");

            // Bắt buộc phải có Giá tiền và Rating để AI tư vấn và nhét vào JSON Frontend
            // map.put("price", p.getPrice()); // Đã mở comment
            map.put("rating", 5); // Có thể thay bằng p.getRating() nếu DB của bạn có

            // Sinh sẵn URL để AI ném về cho Frontend (Thay đổi /hotel/ thành route của bạn)
            map.put("bookingUrl", "/hotels/" + p.getPropertyId());

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(optimizedResults);
    }
}
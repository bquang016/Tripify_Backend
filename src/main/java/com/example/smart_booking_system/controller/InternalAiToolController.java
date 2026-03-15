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

    // Lấy secret key từ file config
    @Value("${ai.internal.secret}")
    private String internalSecret;

    @GetMapping("/search-hotels")
    public ResponseEntity<?> searchHotels(
            @RequestHeader("X-Internal-Secret") String secretHeader, // Bắt buộc phải có header này
            @RequestParam String city,
            @RequestParam int capacity) {

        // 1. Kiểm tra xem n8n có đưa đúng mật khẩu không
        if (!internalSecret.equals(secretHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Sai khóa bảo mật nội bộ!");
        }

        // 2. Lấy dữ liệu từ Database
        List<Property> properties = propertyDetailRepository.findAvailableProperties(city.toLowerCase(), capacity);

        // 3. TỐI ƯU DTO CHO AI: Chỉ map các trường thật sự cần thiết để AI đọc
        // Lưu ý: Các hàm get() dưới đây giả định theo entity Property của bạn, bạn có
        // thể thêm/bớt cho phù hợp
        List<Map<String, Object>> optimizedResults = properties.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("propertyId", p.getPropertyId());
            map.put("name", p.getPropertyName());
            map.put("address", p.getAddress() != null ? p.getAddress() : "Đang cập nhật");
            // map.put("price", p.getBasePrice()); // Mở comment nếu Property có trường giá
            // map.put("thumbnail", p.getThumbnailUrl()); // Mở comment nếu có ảnh
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(optimizedResults);
    }
}
package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.repository.PropertyDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

        // Kiểm tra xem n8n có đưa đúng mật khẩu không
        if (!internalSecret.equals(secretHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Sai khóa bảo mật nội bộ!");
        }

        // Nếu đúng mật khẩu, cho phép lấy dữ liệu
        List<Property> properties = propertyDetailRepository.findAvailableProperties(city.toLowerCase(), capacity);
        return ResponseEntity.ok(properties);
    }
}
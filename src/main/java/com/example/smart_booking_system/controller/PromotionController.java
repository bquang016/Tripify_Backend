package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.BookingResponseDTO;
import com.example.smart_booking_system.dto.PromotionResponseDTO;
import com.example.smart_booking_system.dto.request.promotion.PromotionRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // 1. TẠO MỚI
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> create(@Valid @RequestBody PromotionRequestDTO req) {
        try {
            return ResponseEntity.status(201).body(
                    ApiResponse.success("Tạo mã khuyến mãi thành công", promotionService.createPromotion(req))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 2. CẬP NHẬT
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> update(@PathVariable int id, @Valid @RequestBody PromotionRequestDTO req) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success("Cập nhật thành công", promotionService.updatePromotion(id, req))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 3. XÓA
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> delete(@PathVariable int id) {
        try {
            promotionService.deletePromotion(id);
            return ResponseEntity.ok(ApiResponse.success("Đã xóa mã khuyến mãi"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 4. TOGGLE STATUS
    @PutMapping("/toggle/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> toggle(@PathVariable int id) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success("Thay đổi trạng thái thành công", promotionService.toggleStatus(id))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 5. LẤY TẤT CẢ (TOÀN SÀN - ADMIN)
    @GetMapping("/all")
    public ResponseEntity<?> getAllGlobalPromotions() {
        return ResponseEntity.ok(
                ApiResponse.success(promotionService.getAllGlobalPromotions())
        );
    }

    // 6. LẤY MÃ CỦA PROPERTY CỤ THỂ
    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> getPromotionsByProperty(@PathVariable int propertyId) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success(promotionService.getPromotionsByProperty(propertyId))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 7. CHI TIẾT
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getDetail(@PathVariable int id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(promotionService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 8. UPLOAD BANNER
    @PostMapping(value = "/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> uploadBanner(
            @PathVariable int id,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Cập nhật banner thành công",
                            promotionService.uploadBanner(id, file)
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 9. GỢI Ý MÃ (USER)
    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<PromotionResponseDTO>> suggestPromotion(
            @RequestParam String userId,
            @RequestParam(required = false) Integer propertyId,
            @RequestParam BigDecimal amount) {

        PromotionResponseDTO bestPromo = promotionService.suggestBestPromotion(userId, propertyId, amount);

        if (bestPromo != null) {
            return ResponseEntity.ok(ApiResponse.success("Tìm thấy mã phù hợp", bestPromo));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Không có mã phù hợp", null));
        }
    }

    // 10. LẤY DANH SÁCH CHO OWNER
    @GetMapping("/owner/my-promotions")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> getOwnerPromotions() {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success(promotionService.getPromotionsByCurrentOwner())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
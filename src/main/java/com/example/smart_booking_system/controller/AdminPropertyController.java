package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.PropertyResponseDTO; // Đảm bảo bạn có DTO này
import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.dto.request.admin.SuspendRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.service.PropertyService;
import com.example.smart_booking_system.service.RoomService; // Inject thêm RoomService
import com.example.smart_booking_system.security.CheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/properties")
@PreAuthorize("hasRole('ADMIN')")
@CheckPermission("PROPERTY_MANAGE")
public class AdminPropertyController {

    private final PropertyService propertyService;
    private final RoomService roomService; // Inject service quản lý phòng

    // ============================================================
    // 1. CÁC API CŨ (GIỮ NGUYÊN)
    // ============================================================

    // Lấy danh sách theo trạng thái (PENDING, REJECTED...)
    @GetMapping("/status")
    public ResponseEntity<?> getPropertiesByStatus(@RequestParam String status) {
        try {
            PropertyStatus propertyStatus = PropertyStatus.valueOf(status.toUpperCase());
            List<PropertyDetailDTO> properties = propertyService.getPropertiesByStatus(propertyStatus);
            return ResponseEntity.ok(properties);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái không hợp lệ: " + status));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // Duyệt hoặc Từ chối Property
    @PostMapping("/{propertyId}/review")
    public ResponseEntity<?> reviewProperty(
            @PathVariable Integer propertyId,
            @Valid @RequestBody PropertyReviewDTO reviewDTO,
            Authentication authentication
    ) {
        try {
            String adminUsername = (authentication != null) ? authentication.getName() : "Admin";
            PropertyDetailDTO reviewedProperty = propertyService.reviewProperty(propertyId, reviewDTO, adminUsername);

            String message = (reviewDTO.getStatus() == PropertyStatus.APPROVE)
                    ? "Duyệt cơ sở thành công"
                    : "Từ chối cơ sở thành công";

            return ResponseEntity.ok(ApiResponse.success(message, reviewedProperty));
        } catch (ResourceNotFoundException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // ============================================================
    // 2. CÁC API MỚI (CHỨC NĂNG SUSPEND)
    // ============================================================

    // [NEW] Lấy danh sách Property đang hoạt động (ACTIVE/APPROVED) có phân trang
    @GetMapping("/active")
    public ResponseEntity<ApiResponse> getActiveProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PropertyResponseDTO> properties = propertyService.getAllActiveProperties(pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành công", properties));
    }

    // [NEW] Dừng hoạt động (Suspend) một khách sạn
    @PutMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse> suspendProperty(
            @PathVariable Integer id,
            @RequestBody @Valid SuspendRequestDTO request
    ) {
        propertyService.suspendProperty(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Đã dừng hoạt động cơ sở lưu trú", null));
    }

    // [NEW] Lấy danh sách phòng của một khách sạn cụ thể
    @GetMapping("/{id}/rooms")
    public ResponseEntity<ApiResponse> getPropertyRooms(@PathVariable Integer id) {
        // [FIX] Gọi hàm mới: getAllRoomsByPropertyId (lấy tất cả active/inactive/suspended)
        List<RoomResponseDTO> rooms = roomService.getAllRoomsByPropertyId(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng thành công", rooms));
    }

    // [NEW] Dừng hoạt động (Suspend) một phòng cụ thể
    // URL: /api/v1/admin/properties/rooms/{roomId}/suspend
    @PutMapping("/rooms/{roomId}/suspend")
    public ResponseEntity<ApiResponse> suspendRoom(
            @PathVariable Integer roomId,
            @RequestBody @Valid SuspendRequestDTO request
    ) {
        roomService.suspendRoom(roomId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Đã dừng hoạt động phòng", null));
    }

    // 2. API Kích hoạt lại Khách sạn
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse> activateProperty(@PathVariable Integer id) {
        propertyService.activateProperty(id);
        return ResponseEntity.ok(ApiResponse.success("Đã mở lại hoạt động cơ sở lưu trú", null));
    }

    // 3. API Kích hoạt lại Phòng
    @PutMapping("/rooms/{roomId}/activate")
    public ResponseEntity<ApiResponse> activateRoom(@PathVariable Integer roomId) {
        roomService.activateRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success("Đã mở lại hoạt động phòng", null));
    }

    @GetMapping("/list") // Đổi tên path để rõ ràng hơn, hoặc dùng lại /active nhưng thêm param
    public ResponseEntity<ApiResponse> getPropertiesList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "APPROVE") String status // Thêm tham số này
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PropertyStatus propertyStatus = PropertyStatus.valueOf(status);

        // Đảm bảo Repository có hàm findByPropertyStatus trả về Page
        Page<PropertyResponseDTO> properties = propertyService.getPropertiesByStatusPaginated(propertyStatus, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành công", properties));
    }
}
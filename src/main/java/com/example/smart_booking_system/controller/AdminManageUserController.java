package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.admin.AdminUserResponseDTO;
import com.example.smart_booking_system.service.AdminManageUserService;
import com.example.smart_booking_system.security.CheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@CheckPermission("USER_MANAGE")
public class AdminManageUserController {

    private final AdminManageUserService adminManageUserService;

    // 1. Lấy danh sách Users (Phân trang + Lọc)
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String rank // Thêm dòng này
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Truyền thêm biến rank vào service
        Page<AdminUserResponseDTO> users = adminManageUserService.getAllUsers(keyword, role, status, rank, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành công", users));
    }

    // 2. Xem chi tiết User
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserDetail(@PathVariable String userId) {
        AdminUserResponseDTO user = adminManageUserService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin user thành công", user));
    }

    // 3. Khóa / Mở khóa User (Status)
    @PatchMapping("/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable String userId,
            @RequestParam String status,
            @RequestParam(required = false) String reason // Thêm tham số này
    ) {
        try {
            // Truyền reason vào service
            adminManageUserService.updateUserStatus(userId, status, reason);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công: " + status, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
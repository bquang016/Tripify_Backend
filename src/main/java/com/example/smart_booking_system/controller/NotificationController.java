package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 1. Lấy danh sách (Thêm param scope)
    // GET /api/v1/notifications?scope=CUSTOMER&page=0
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String scope
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getUserNotifications(userDetails.getUserId(), scope, pageable));
    }

    // 2. Đếm số lượng chưa đọc (Thêm param scope)
    // GET /api/v1/notifications/unread-count?scope=CUSTOMER
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "ALL") String scope
    ) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userDetails.getUserId(), scope));
    }

    // 3. Đánh dấu 1 cái đã đọc
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAsRead(id, userDetails.getUserId());
        return ResponseEntity.ok("Marked as read");
    }

    // 4. Đánh dấu tất cả đã đọc (Thêm param scope)
    // PUT /api/v1/notifications/read-all?scope=CUSTOMER
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "ALL") String scope
    ) {
        notificationService.markAllAsRead(userDetails.getUserId(), scope);
        return ResponseEntity.ok("All marked as read for scope: " + scope);
    }
}
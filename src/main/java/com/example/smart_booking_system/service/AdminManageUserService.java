package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.admin.AdminUserResponseDTO;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.example.smart_booking_system.enums.MembershipRank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManageUserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    // 1. Lấy danh sách user
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDTO> getAllUsers(String keyword, String role, String status, String rankStr, Pageable pageable) {
        MembershipRank rank = null;
        if (rankStr != null && !rankStr.isEmpty()) {
            try {
                rank = MembershipRank.valueOf(rankStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                rank = null;
            }
        }

        Page<User> userPage = userRepository.findUsersWithFilter(keyword, role, status, rank, pageable);

        return userPage.map(this::convertToDTO);
    }

    // 2. Lấy chi tiết user
    @Transactional(readOnly = true)
    public AdminUserResponseDTO getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return convertToDTO(user);
    }

    // 3. Cập nhật trạng thái (Khóa/Mở khóa)
    @Transactional
    public void updateUserStatus(String userId, String status, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newStatus = status.toUpperCase();
        user.setStatus(newStatus);
        userRepository.save(user);

        // Kiểm tra nếu là hành động KHÓA thì gửi mail
        if ("BANNED".equals(newStatus) || "SUSPENDED".equals(newStatus)) {
            if (reason == null || reason.trim().isEmpty()) {
                // Nếu khóa mà không nhập lý do -> Gán lý do mặc định hoặc Báo lỗi tùy bạn
                reason = "Vi phạm điều khoản sử dụng của hệ thống.";
            }
            // Gửi email
            emailService.sendAccountLockedEmail(user.getEmail(), user.getFullName(), reason);
        }
    }

    // --- Helper Methods ---

    private AdminUserResponseDTO convertToDTO(User user) {
        return new AdminUserResponseDTO(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getIsEmailVerified(),
                user.getPoints(),
                user.getMembershipRank(),
                user.getCreatedAt(),
                user.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.toSet())
        );
    }
}
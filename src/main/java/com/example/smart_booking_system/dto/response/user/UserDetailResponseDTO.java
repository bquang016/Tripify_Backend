package com.example.smart_booking_system.dto.response.user;

import com.example.smart_booking_system.enums.MembershipRank;
import lombok.Data;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
public class UserDetailResponseDTO {
    // Các trường từ User
    private String userId;
    private String email;
    private String fullName;
    private String phoneNumber;

    // Các trường từ UserDetail
    private Integer userdetailId;
    private String gender;
    private LocalDate dateOfBirth; // <-- ĐÃ THÊM
    private String profilePhotoUrl;
    private String address;
    private String city;
    private String country;
    private int points;
    private MembershipRank membershipRank;

    private String notificationEmail;
    private List<SocialAccountDTO> socialAccounts; // Để hiển thị list email phụ

    // Tạo thêm class DTO nhỏ ở trong hoặc ngoài file này để map SocialAccount
    @Data
    @AllArgsConstructor
    public static class SocialAccountDTO {
        private String provider;
        private String email;
    }
}
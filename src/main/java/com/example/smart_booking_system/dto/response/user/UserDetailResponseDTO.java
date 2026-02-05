package com.example.smart_booking_system.dto.response.user;

import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.MembershipRank;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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
    private LocalDate dateOfBirth;
    private String profilePhotoUrl;
    private String address;
    private String city;
    private String country;
    private int points;
    private MembershipRank membershipRank;

    private String notificationEmail;
    private List<SocialAccountDTO> socialAccounts;

    @Data
    @AllArgsConstructor
    public static class SocialAccountDTO {
        private String provider;
        private String email;
    }

    // --- HÀM MỚI ĐÃ ĐƯỢC THÊM ĐỂ FIX LỖI ---
    public static UserDetailResponseDTO fromEntity(User user) {
        UserDetailResponseDTO dto = new UserDetailResponseDTO();
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());

        if (user.getUserDetail() != null) {
            dto.setUserdetailId(user.getUserDetail().getUserdetailId());
            dto.setGender(user.getUserDetail().getGender());
            dto.setDateOfBirth(user.getUserDetail().getDateOfBirth());
            dto.setProfilePhotoUrl(user.getUserDetail().getProfilePhotoUrl());
            dto.setAddress(user.getUserDetail().getAddress());
            dto.setCity(user.getUserDetail().getCity());
            dto.setCountry(user.getUserDetail().getCountry());
            dto.setPoints(user.getUserDetail().getPoints());
            dto.setMembershipRank(user.getUserDetail().getMembershipRank());
            dto.setNotificationEmail(user.getUserDetail().getNotificationEmail());
        }

        if (user.getSocialAccounts() != null) {
            dto.setSocialAccounts(user.getSocialAccounts().stream()
                    .map(acc -> new SocialAccountDTO(acc.getProvider().toString(), acc.getEmail()))
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
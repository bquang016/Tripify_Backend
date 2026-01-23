package com.example.smart_booking_system.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.smart_booking_system.enums.MembershipRank;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserResponseDTO {
    private String userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String status;
    private Boolean isEmailVerified;
    private int points;
    private MembershipRank membershipRank;
    private LocalDateTime createdAt;
    private Set<String> roles;
}
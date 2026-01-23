package com.example.smart_booking_system.dto.request.user;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserDetailRequestDTO {
    // Các trường từ User
    private String fullName;
    private String phoneNumber;

    // Các trường từ UserDetail
    private String gender;
    private LocalDate dateOfBirth; // <-- ĐÃ THÊM
    private String profilePhotoUrl;
    private String address;
    private String city;
    private String country;
    private String notificationEmail;
}
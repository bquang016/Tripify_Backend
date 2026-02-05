package com.example.smart_booking_system.dto.request.auth;

import lombok.Data;

@Data
public class OwnerRegisterRequest {
    private String email;
    private String otp;
    private String password; // Owner cần mật khẩu để đăng nhập sau này
}
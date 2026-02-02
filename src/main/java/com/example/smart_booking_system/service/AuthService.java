package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.auth.*;
import com.example.smart_booking_system.dto.response.auth.LoginResponse;
import com.example.smart_booking_system.entity.User;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void verifyEmail(String token);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request, String userId);

    void resendVerificationEmail(String email);

    void unlinkSocialAccount(String userId, String providerName);
    void createPassword(String userId, String newPassword);
    User getCurrentUser();

    void sendOtp(String email);

    boolean verifyOtp(String email, String code);
}

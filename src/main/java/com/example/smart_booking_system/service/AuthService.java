package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.auth.*;
import com.example.smart_booking_system.dto.response.auth.LoginResponse;
import com.example.smart_booking_system.entity.User;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse verifyRegisterOtp(VerifyOtpRequest request);

    LoginResponse login(LoginRequest request);

    void verifyEmail(String token);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request, String userId);

    void resendVerificationEmail(String email);

    void unlinkSocialAccount(String userId, String providerName);
    void createPassword(String userId, String newPassword);
    User getCurrentUser();

    // 2FA Methods
    void request2faToggle(String userId);
    void verify2faToggle(String userId, String otp);
    LoginResponse verify2faLogin(VerifyOtpRequest request);

    void sendOtp(String email, com.example.smart_booking_system.enums.OtpType type);

    String verifyOtp(String email, String code, com.example.smart_booking_system.enums.OtpType type);

    void sendOwnerRegistrationOtp(String email);
    LoginResponse verifyOwnerOtpAndRegister(OwnerRegisterRequest request);
}

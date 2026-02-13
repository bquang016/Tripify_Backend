package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.auth.*;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.auth.VerifyOwnerOtpResponse;
import com.example.smart_booking_system.dto.response.auth.LoginResponse;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.security.JwtTokenProvider;
import com.example.smart_booking_system.service.AuthService;
import com.example.smart_booking_system.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra để hoàn tất đăng ký."));
    }

    @PostMapping("/verify-register")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyRegister(@Valid @RequestBody VerifyOtpRequest request) {
        LoginResponse response = authService.verifyRegisterOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Date exp = jwtTokenProvider.extractExpiration(token);
            tokenBlacklistService.blacklist(token, exp.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("token") String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email verified successfully"
            ));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }



    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Verification email sent. Please check your inbox."));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpRequest request) {
        authService.sendOtp(request.getEmail(), request.getType());
        return ResponseEntity.ok(ApiResponse.success("OTP has been sent to your email."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String token = authService.verifyOtp(request.getEmail(), request.getOtp(), request.getType());
        if (token != null) {
            return ResponseEntity.ok(ApiResponse.success("Xác thực OTP thành công.", token));
        } else {
            throw new BadRequestException("Mã OTP không hợp lệ hoặc đã hết hạn.");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to your email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful. You can now login with your new password."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        authService.changePassword(request, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomUserDetails>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(currentUser));
    }

    @DeleteMapping("/social/unlink")
    public ResponseEntity<ApiResponse<Void>> unlinkSocialAccount(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam String provider) {

        authService.unlinkSocialAccount(currentUser.getUserId(), provider);

        return ResponseEntity.ok(ApiResponse.success("Đã ngắt kết nối tài khoản " + provider));
    }
    @PostMapping("/create-password")
    public ResponseEntity<ApiResponse<Void>> createPassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody Map<String, String> request) { // Nhận JSON { "password": "..." }

        String newPassword = request.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 6 ký tự.");
        }

        authService.createPassword(currentUser.getUserId(), newPassword);
        return ResponseEntity.ok(ApiResponse.success("Tạo mật khẩu thành công"));
    }

    // --- Owner Registration Flow ---

    @PostMapping("/owner/check-email")
    public ResponseEntity<ApiResponse<Void>> checkOwnerEmail(@Valid @RequestBody CheckEmailRequest request) {
        authService.checkOwnerEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Email is available for registration."));
    }

    @PostMapping("/owner/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOwnerOtp(@Valid @RequestBody CheckEmailRequest request) {
        authService.sendOwnerOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Mã OTP đã được gửi đến email.", null));
    }

    @PostMapping("/owner/verify-otp")
    public ResponseEntity<ApiResponse<VerifyOwnerOtpResponse>> verifyOwnerOtp(@Valid @RequestBody VerifyOtpRequest request) {
        VerifyOwnerOtpResponse response = authService.verifyOwnerOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verification successful.", response));
    // --- 2FA ENDPOINTS ---

    @PostMapping("/2fa/request-toggle")
    public ResponseEntity<ApiResponse<Void>> request2faToggle(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        authService.request2faToggle(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Mã OTP xác thực 2 bước đã được gửi đến email của bạn."));
    }

    @PostMapping("/2fa/toggle")
    public ResponseEntity<ApiResponse<Void>> toggle2fa(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody Map<String, String> request) {
        String otp = request.get("otp");
        if (otp == null) throw new BadRequestException("Mã OTP là bắt buộc");
        
        authService.verify2faToggle(currentUser.getUserId(), otp);
        return ResponseEntity.ok(ApiResponse.success("Cấu hình xác thực 2 bước đã được cập nhật thành công."));
    }

    @PostMapping("/2fa/verify-login")
    public ResponseEntity<ApiResponse<LoginResponse>> verify2faLogin(
            @Valid @RequestBody VerifyOtpRequest request) {
        LoginResponse response = authService.verify2faLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Xác thực thành công", response));
    }
}
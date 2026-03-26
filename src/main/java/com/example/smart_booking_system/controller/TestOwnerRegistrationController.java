package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.entity.OwnerApplication;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.OwnerApplicationRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.service.OwnerApplicationService;
import com.example.smart_booking_system.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test/owner")
// CỰC KỲ QUAN TRỌNG: Đảm bảo API này chỉ kích hoạt ở môi trường test/dev
@Profile({"dev", "test", "local"})
@RequiredArgsConstructor
public class TestOwnerRegistrationController {

    private final OwnerApplicationService ownerApplicationService;
    private final OwnerApplicationRepository ownerApplicationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * YÊU CẦU 1: Bỏ qua bước gửi email thực tế và mặc định OTP là "123456"
     * Khi gọi API này với OTP "123456", hệ thống sẽ trả về luôn temporaryToken
     * để front-end dùng gọi API /api/v1/owner/register/submit tiếp theo.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> testVerifyOtp(
            @RequestParam String email,
            @RequestParam String otp) {

        if ("123456".equals(otp)) {
            // Giả lập việc tạo Temporary Token như luồng thật
            // (Lưu ý: Bạn có thể cần điều chỉnh lại tên hàm tạo token tùy thuộc vào class JwtTokenProvider của bạn đang viết hàm tên gì)
            // Đoạn này sinh ra token có claim "type" = "temporary" theo đúng yêu cầu ở OwnerRegistrationServiceImpl
            String temporaryToken = jwtTokenProvider.generateTemporaryTokenForTest(email);

            return ResponseEntity.ok(ApiResponse.success(
                    "Xác thực OTP thành công (TEST MODE)",
                    "Bearer " + temporaryToken
            ));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("OTP không hợp lệ"));
    }

    /**
     * YÊU CẦU 2: Phê duyệt đơn tự động và ghi đè mật khẩu thành "Matkhau123@"
     * Chạy API này thay vì API duyệt đơn mặc định của Admin.
     */
    @PostMapping("/approve/{applicationId}")
    public ResponseEntity<ApiResponse<Void>> testApproveApplication(@PathVariable Long applicationId) {
        try {
            // Bước 1: Vẫn gọi hàm approve mặc định của hệ thống để thực thi toàn bộ logic (tạo Wallet, Property, Rooms...)
            ownerApplicationService.approveApplication(applicationId, "admin_test");

            // Bước 2: Tìm lại đơn đăng ký để lấy thông tin email
            OwnerApplication application = ownerApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký"));

            // Bước 3: Tìm User vừa được tạo ra (từ email của đơn) và ghi đè mật khẩu
            User owner = userRepository.findByEmail(application.getEmail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản owner vừa tạo"));

            owner.setPasswordHash(passwordEncoder.encode("Matkhau123@"));
            userRepository.save(owner);

            return ResponseEntity.ok(ApiResponse.success(
                    "Phê duyệt thành công và đã set mật khẩu mặc định: Matkhau123@",
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi khi duyệt đơn test: " + e.getMessage()));
        }
    }
}
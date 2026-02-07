package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.owner.OwnerProfileRequest;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.UserDetailService;
import com.fasterxml.jackson.databind.ObjectMapper; // Cần import cái này để parse JSON string
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/owner/onboarding")
@RequiredArgsConstructor
public class OwnerOnboardingController {

    private final UserDetailService userDetailService;
    private final UserRepository userRepository;

    // SỬA: Chuyển sang nhận MultipartFile
    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            // Nhận JSON string và convert sang Object (vì FormData không gửi lồng Object được)
            @RequestPart("data") String dataString,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart(value = "cccdFront") MultipartFile cccdFront,
            @RequestPart(value = "cccdBack") MultipartFile cccdBack
    ) {
        try {
            // 1. Convert String JSON -> DTO
            ObjectMapper mapper = new ObjectMapper();
            // Cần đăng ký module JavaTimeModule nếu DTO có LocalDate
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            OwnerProfileRequest request = mapper.readValue(dataString, OwnerProfileRequest.class);

            // 2. Lấy User
            User user = userRepository.findById(currentUser.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 3. Gọi Service (Cần sửa Service để nhận thêm file ảnh)
            userDetailService.updateOwnerProfileWithImages(user, request, avatar, cccdFront, cccdBack);

            return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi cập nhật: " + e.getMessage()));
        }
    }
}
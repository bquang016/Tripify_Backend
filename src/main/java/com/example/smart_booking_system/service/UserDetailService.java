package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.user.UserDetailRequestDTO;
import com.example.smart_booking_system.dto.response.user.UserDetailResponseDTO;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.UserDetail;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.UserDetailRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.dto.request.owner.OwnerProfileRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailService {

    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;
    private final FileStorageService fileStorageService;

    public UserDetailService(UserRepository userRepository,
                             UserDetailRepository userDetailRepository,
                             FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userDetailRepository = userDetailRepository;
        this.fileStorageService = fileStorageService;
    }

    // ==============================================================
    // GET USER DETAIL — TRẢ VỀ SIGNED URL
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO getUserDetail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newDetail = new UserDetail();
                    newDetail.setUser(user);
                    return userDetailRepository.save(newDetail);
                });

        UserDetailResponseDTO dto = new UserDetailResponseDTO();

        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setPoints(user.getPoints());
        dto.setMembershipRank(user.getMembershipRank());

        dto.setUserdetailId(detail.getUserdetailId());
        dto.setGender(detail.getGender());
        dto.setDateOfBirth(detail.getDateOfBirth());
        dto.setAddress(detail.getAddress());
        dto.setCity(detail.getCity());
        dto.setCountry(detail.getCountry());

        // Avatar: trả về signedURL
        if (StringUtils.hasText(detail.getProfilePhotoUrl())) {
            dto.setProfilePhotoUrl(
                    fileStorageService.generateSignedUrl(detail.getProfilePhotoUrl())
            );
        }

        return dto;
    }

    // ==============================================================
    // UPDATE USER DETAIL
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO updateUserDetail(String email, UserDetailRequestDTO req) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("UserDetail not found for user: " + email));

        user.setFullName(req.getFullName());
        user.setPhoneNumber(req.getPhoneNumber());
        if (StringUtils.hasText(req.getNotificationEmail())) {
            user.setNotificationEmail(req.getNotificationEmail());
        }
        userRepository.save(user);

        detail.setGender(req.getGender());
        detail.setDateOfBirth(req.getDateOfBirth());
        detail.setAddress(req.getAddress());
        detail.setCity(req.getCity());
        detail.setCountry(req.getCountry());

        userDetailRepository.save(detail);

        return this.getUserDetail(email);
    }

    // ==============================================================
    // UPLOAD PROFILE PHOTO — LUÔN LƯU VÀO R2
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO uploadProfilePhoto(String userId, MultipartFile file) {

        // 1. Validate file
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File ảnh không hợp lệ");
        }

        // 2. Lấy User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // 3. Lấy hoặc tạo UserDetail
        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newDetail = new UserDetail();
                    newDetail.setUser(user);
                    return userDetailRepository.save(newDetail);
                });

        // 4. Xóa ảnh cũ (CHỈ KHI LÀ KEY, KHÔNG PHẢI URL)
        String oldValue = detail.getProfilePhotoUrl();
        if (StringUtils.hasText(oldValue) && !oldValue.startsWith("http")) {
            fileStorageService.deleteFile(oldValue);
        }

        // 5. Upload ảnh mới lên R2
        String key = fileStorageService.storeImageFile(file, "userdetail");

        // 6. Lưu KEY vào DB
        detail.setProfilePhotoUrl(key);
        userDetailRepository.save(detail);

        // 7. Trả về DTO (getUserDetail phải convert KEY -> URL)
        return getUserDetail(user.getEmail());
    }
    @Transactional
    public UserDetail updateOwnerProfile(User user, OwnerProfileRequest request) {
        // 1. Cập nhật thông tin cơ bản ở bảng User
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
        }
        if (StringUtils.hasText(request.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        userRepository.save(user);

        // 2. Cập nhật hoặc tạo mới UserDetail
        UserDetail userDetail = userDetailRepository.findByUser(user)
                .orElse(new UserDetail());

        if (userDetail.getUser() == null) {
            userDetail.setUser(user);
        }

        // Các setter này sẽ hết lỗi sau khi bạn làm Bước 1 và Bước 2
        userDetail.setIdentityCardNumber(request.getIdentityCardNumber());
        userDetail.setDateOfBirth(request.getDateOfBirth());
        userDetail.setGender(request.getGender());
        userDetail.setAddress(request.getAddress());
        userDetail.setCity(request.getCity());
        userDetail.setCountry(request.getCountry());

        if (StringUtils.hasText(request.getProfilePhotoUrl())) {
            userDetail.setProfilePhotoUrl(request.getProfilePhotoUrl());
        }

        return userDetailRepository.save(userDetail);
    }
    // Thêm hàm mới trong UserDetailService
    @Transactional
    public UserDetail updateOwnerProfileWithImages(User user, OwnerProfileRequest request,
                                                   MultipartFile avatar,
                                                   MultipartFile cccdFront,
                                                   MultipartFile cccdBack) {

        // 1. Cập nhật User cơ bản
        if (StringUtils.hasText(request.getFullName())) user.setFullName(request.getFullName());
        if (StringUtils.hasText(request.getPhoneNumber())) user.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(user);

        // 2. Lấy UserDetail
        UserDetail detail = userDetailRepository.findByUser(user).orElse(new UserDetail());
        if (detail.getUser() == null) detail.setUser(user);

        // 3. Map dữ liệu
        detail.setIdentityCardNumber(request.getIdentityCardNumber());
        detail.setDateOfBirth(request.getDateOfBirth());
        detail.setGender(request.getGender());
        detail.setAddress(request.getAddress());
        detail.setCity(request.getCity());
        detail.setCountry(request.getCountry());

        // 4. XỬ LÝ UPLOAD ẢNH (Quan trọng)

        // a. Avatar (Không bắt buộc)
        if (avatar != null && !avatar.isEmpty()) {
            String avatarKey = fileStorageService.storeImageFile(avatar, "avatars");
            detail.setProfilePhotoUrl(avatarKey);
        }

        // b. CCCD Mặt trước (Bắt buộc - nên check ở Controller hoặc đây)
        if (cccdFront != null && !cccdFront.isEmpty()) {
            String frontKey = fileStorageService.storeImageFile(cccdFront, "cccd/front");
            // Lưu vào DB (Cần thêm cột cccdFrontUrl trong Entity UserDetail nếu chưa có)
            detail.setCccdFrontUrl(frontKey); // Giả sử bạn đã thêm cột này
        }

        // c. CCCD Mặt sau
        if (cccdBack != null && !cccdBack.isEmpty()) {
            String backKey = fileStorageService.storeImageFile(cccdBack, "cccd/back");
            // Lưu vào DB
            detail.setCccdBackUrl(backKey); // Giả sử bạn đã thêm cột này
        }

        return userDetailRepository.save(detail);
    }


    // ==============================================================
    // CHECK PROFILE COMPLETENESS
    // ==============================================================
    @Transactional(readOnly = true)
    public ProfileStatusResponse checkProfileCompleteness(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        UserDetail detail = userDetailRepository.findByUser(user).orElse(null);

        List<String> missing = new ArrayList<>();

        if (!StringUtils.hasText(user.getFullName())) missing.add("Họ và Tên");
        if (!StringUtils.hasText(user.getPhoneNumber())) missing.add("Số điện thoại");

        if (detail == null) {
            missing.add("Giới tính");
            missing.add("Ngày sinh");
        } else {
            if (!StringUtils.hasText(detail.getGender())) missing.add("Giới tính");
            if (detail.getDateOfBirth() == null) missing.add("Ngày sinh");
        }

        return new ProfileStatusResponse(missing.isEmpty(), missing.isEmpty() ? null : missing);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileStatusResponse {
        private boolean isProfileComplete;
        private List<String> missingFields;
    }
}

package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.enums.NotificationType;
import com.example.smart_booking_system.service.EmailService;
import com.example.smart_booking_system.service.FileStorageService;
import com.example.smart_booking_system.dto.request.application.OwnerApplicationSubmitDTO;
import com.example.smart_booking_system.dto.request.admin.OwnerApplicationReviewDTO;
import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.entity.OwnerApplication;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.ApplicationStatus;
import com.example.smart_booking_system.repository.OwnerApplicationRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.service.OwnerApplicationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.smart_booking_system.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.thymeleaf.context.Context;
import java.time.format.DateTimeFormatter;
import com.example.smart_booking_system.repository.RoleRepository;
import com.example.smart_booking_system.entity.Role;

@Service
@RequiredArgsConstructor
@Transactional
public class OwnerApplicationServiceImpl implements OwnerApplicationService {

    private final OwnerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final RoleRepository roleRepository;
    private final NotificationService notificationService;


    // 🔥 Dùng để upload + generate signed URL
    private final FileStorageService fileStorageService;

    @Override
    public OwnerApplicationDTO submitApplication(OwnerApplicationSubmitDTO submitDTO, String applicantUsername) {

        User applicant = userRepository.findByEmail(applicantUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + applicantUsername));

        OwnerApplication application = new OwnerApplication();
        application.setUserId(applicant);

        application.setPermanentAddress(submitDTO.getPermanentAddress());
        application.setHometownAddress(submitDTO.getHometownAddress());
        application.setBusinessLicenseNumber(submitDTO.getBusinessLicenseNumber());
        application.setPersonalDob(submitDTO.getPersonalDob());
        application.setPersonalIdCard(submitDTO.getPersonalIdCard());

        // ============================
        // 📌 UPLOAD ẢNH (Giữ nguyên logic upload)
        // ============================
        String frontUrl = fileStorageService.storeImageFile(
                submitDTO.getCardFrontImage(),
                "owner-applications/card-front"
        );

        String backUrl = fileStorageService.storeImageFile(
                submitDTO.getCardBackImage(),
                "owner-applications/card-back"
        );

        String licenseUrl = fileStorageService.storeImageFile(
                submitDTO.getBusinessLicenseImage(),
                "owner-applications/business-license"
        );

        application.setCardFrontImage(frontUrl);
        application.setCardBackImage(backUrl);
        application.setBusinessLicenseImage(licenseUrl);

        OwnerApplication savedApp = applicationRepository.save(application);

        try {
            notificationService.sendToAllAdmins(
                    "Đơn đăng ký Owner mới",
                    "Người dùng " + applicant.getFullName() + " vừa nộp đơn đăng ký đối tác.",
                    NotificationType.ADMIN_NEW_OWNER_REGISTRATION,
                    savedApp.getId().toString()
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo admin: " + e.getMessage());
        }

        // Email giữ nguyên
        try {
            String subject = "Xác nhận nộp đơn đăng ký làm chủ khách sạn";
            String templateName = "email/application-submitted-confirmation";

            Context context = new Context();
            context.setVariable("applicantName", applicant.getFullName());
            context.setVariable("applicationId", savedApp.getId());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
            context.setVariable("submittedAt", savedApp.getCreatedAt().format(formatter));

            emailService.sendHtmlEmail(applicant.getEmail(), subject, templateName, context);

        } catch (Exception e) {
            System.err.println("Lỗi gửi email xác nhận nộp đơn: " + e.getMessage());
        }

        return convertToDTO(savedApp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerApplicationDTO> getPendingOwnerApplications() {
        return this.getApplicationsByStatus(ApplicationStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerApplicationDTO> getApplicationsByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatusWithApplicant(status)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OwnerApplicationDTO reviewApplication(Long applicationId, OwnerApplicationReviewDTO reviewDTO, String adminUsername) {

        User admin = userRepository.findByEmail(adminUsername)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found: " + adminUsername));

        OwnerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Đơn này đã được xử lý rồi.");
        }

        ApplicationStatus newStatus = reviewDTO.getStatus();
        if (newStatus == null)
            throw new IllegalArgumentException("Trạng thái mới không được để trống.");

        if (newStatus != ApplicationStatus.APPROVED && newStatus != ApplicationStatus.REJECTED)
            throw new IllegalArgumentException("Chỉ được chuyển sang APPROVED hoặc REJECTED.");

        application.setAdminReason(reviewDTO.getReason());
        application.setStatus(newStatus);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(admin);

        User applicant = application.getUserId();

        if (newStatus == ApplicationStatus.APPROVED) {
            Role ownerRole = roleRepository.findByName("OWNER")
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_OWNER"));

            applicant.addRole(ownerRole);
            userRepository.save(applicant);
        }

        OwnerApplication savedApp = applicationRepository.save(application);

        // [BỔ SUNG] Gửi thông báo In-App cho User (Applicant)
        if (applicant != null) {
            // 1. Gửi email (code cũ)
            sendReviewNotificationEmail(applicant, savedApp);

            // 2. Gửi thông báo hệ thống (code mới)
            try {
                if (newStatus == ApplicationStatus.APPROVED) {
                    notificationService.sendNotification(
                            applicant.getUserId(),
                            "Đơn đăng ký Owner được chấp thuận",
                            "Chúc mừng! Bạn đã chính thức trở thành đối tác. Hãy bắt đầu đăng tải khách sạn ngay.",
                            NotificationType.APPROVAL,
                            savedApp.getId().toString()
                    );
                } else if (newStatus == ApplicationStatus.REJECTED) {
                    notificationService.sendNotification(
                            applicant.getUserId(),
                            "Đơn đăng ký Owner bị từ chối",
                            "Lý do: " + (reviewDTO.getReason() != null ? reviewDTO.getReason() : "Không có lý do cụ thể"),
                            NotificationType.REJECTION,
                            savedApp.getId().toString()
                    );
                }
            } catch (Exception e) {
                System.err.println("Lỗi gửi thông báo user: " + e.getMessage());
            }
        }

        return convertToDTO(savedApp);
    }

    // ============================================================
    // 🔥 FIX QUAN TRỌNG: Trả về Signed URL cho 3 ảnh
    // ============================================================
    private OwnerApplicationDTO convertToDTO(OwnerApplication app) {

        OwnerApplicationDTO dto = new OwnerApplicationDTO();
        dto.setId(app.getId());
        dto.setStatus(app.getStatus());
        dto.setPermanentAddress(app.getPermanentAddress());
        dto.setHometownAddress(app.getHometownAddress());

        // ============================
        // 🔥 Ảnh trả về signed URL để FE load được
        // ============================
        dto.setCardFrontImage(
                fileStorageService.generateSignedUrl(app.getCardFrontImage())
        );

        dto.setCardBackImage(
                fileStorageService.generateSignedUrl(app.getCardBackImage())
        );

        dto.setBusinessLicenseImage(
                fileStorageService.generateSignedUrl(app.getBusinessLicenseImage())
        );

        dto.setBusinessLicenseNumber(app.getBusinessLicenseNumber());
        dto.setApplicantDob(app.getPersonalDob());
        dto.setPersonalIdCard(app.getPersonalIdCard());
        dto.setCreatedAt(app.getCreatedAt());
        dto.setReviewedAt(app.getReviewedAt());
        dto.setAdminReason(app.getAdminReason());

        if (app.getUserId() != null) {
            User applicant = app.getUserId();

            dto.setApplicantId(applicant.getUserId());
            dto.setApplicantFullName(applicant.getFullName());
            dto.setApplicantEmail(applicant.getEmail());
            dto.setApplicantPhoneNumber(applicant.getPhoneNumber());

            if (applicant.getUserDetail() != null &&
                    applicant.getUserDetail().getProfilePhotoUrl() != null) {

                dto.setApplicantAvatar(
                        fileStorageService.generateSignedUrl(
                                applicant.getUserDetail().getProfilePhotoUrl()
                        )
                );
            }
        }

        if (app.getReviewedBy() != null) {
            dto.setReviewedByAdminName(app.getReviewedBy().getFullName());
        }

        return dto;
    }

    private void sendReviewNotificationEmail(User applicant, OwnerApplication application) {

        String applicantEmail = applicant.getEmail();
        String applicantName = applicant.getFullName();
        String adminReason = application.getAdminReason();
        ApplicationStatus status = application.getStatus();

        Context context = new Context();
        context.setVariable("applicantName", applicantName);
        context.setVariable("reason", adminReason != null && !adminReason.isEmpty() ? adminReason : "N/A");

        String subject, templateName;

        if (status == ApplicationStatus.APPROVED) {
            subject = "Chúc mừng! Đơn đăng ký làm chủ khách sạn của bạn đã được DUYỆT";
            templateName = "email/application-approved";

        } else if (status == ApplicationStatus.REJECTED) {
            subject = "Thông báo: Đơn đăng ký làm chủ khách sạn của bạn đã bị TỪ CHỐI";
            templateName = "email/application-rejected";

        } else {
            return;
        }

        emailService.sendHtmlEmail(applicantEmail, subject, templateName, context);
    }
}

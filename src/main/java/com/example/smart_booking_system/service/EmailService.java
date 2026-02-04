package com.example.smart_booking_system.service;

import jakarta.mail.MessagingException;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;

public interface EmailService {

    void sendVerificationEmail(String toEmail, String fullName, String verificationToken);

    void sendResetPasswordEmail(String toEmail, String fullName, String resetToken);

    void sendBookingConfirmationEmail(String toEmail, String fullName, String bookingId);

    void sendOwnerApplicationNotification(String adminEmail, String applicantName, String applicationId);

    void sendApplicationStatusEmail(String toEmail, String fullName, String status, String reason);

    void sendHtmlEmail(String to, String subject, String templateName, Context context);

    void sendPaymentReminderEmail(String toEmail, String fullName, String bookingId, String totalPrice);

    // Sửa dòng này
    void sendCancellationRequestReceivedEmail(String toEmail, String fullName, String bookingId, BigDecimal totalPrice, BigDecimal penalty, BigDecimal refund);

    void sendCancellationSuccessEmail(String toEmail, String fullName, String bookingId, String refundAmount, String penaltyAmount);

    void sendCheckinReminderEmail(String toEmail, String fullName, String bookingId, String propertyName, String checkInDate);

    void sendThankYouEmail(String toEmail, String fullName, String bookingId, String propertyName);

    // THÊM MỚI: Gửi mail thông báo dừng hoạt động Khách sạn
    void sendPropertySuspensionEmail(String to, String ownerName, String propertyName, String reason);
    void sendRoomSuspensionEmail(String to, String ownerName, String propertyName, String roomName, String reason);

    void sendPropertyReactivationEmail(String to, String ownerName, String propertyName);
    void sendRoomReactivationEmail(String to, String ownerName, String propertyName, String roomName);
    void sendAccountLockedEmail(String to, String name, String reason);
    void sendRefundRejectionEmail(String toEmail, String fullName, String bookingId, String rejectionReason);

    void sendOtpEmail(String toEmail, String otpCode, com.example.smart_booking_system.enums.OtpType type);
}

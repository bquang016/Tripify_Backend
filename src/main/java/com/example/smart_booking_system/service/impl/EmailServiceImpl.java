package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * ✅ Helper: Đảm bảo URL FE luôn có dấu "/" ở cuối
     */
    private String getFrontendBaseUrl() {
        return frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";
    }

    // ==================================================
    // 🔹 2. Gửi email đặt lại mật khẩu
    // ==================================================
    @Override
    @Async
    public void sendResetPasswordEmail(String toEmail, String fullName, String resetToken) {
        try {
            String subject = "Đặt lại mật khẩu - Smart Booking";

            String encodedToken = URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
            String resetPasswordUrl = getFrontendBaseUrl() + "reset-password?token=" + encodedToken;

            Context context = new Context();
            context.setVariable("username", fullName);
            context.setVariable("resetPasswordUrl", resetPasswordUrl);

            String htmlContent = templateEngine.process("email/reset-password-email", context);
            sendHtmlEmailInternal(toEmail, subject, htmlContent);

        } catch (Exception e) {
            System.err.println("❌ Failed to send reset password email: " + e.getMessage());
        }
    }

    // ==================================================
    // 🔹 3. Gửi xác nhận đặt phòng
    // ==================================================
    @Override
    @Async
    public void sendBookingConfirmationEmail(String toEmail, String fullName, String bookingId) {
        try {
            String subject = "Xác nhận đặt chỗ - Smart Booking";

            Context context = new Context();
            context.setVariable("username", fullName);
            context.setVariable("bookingId", bookingId);
            context.setVariable("bookingUrl", getFrontendBaseUrl() + "bookings/" + bookingId);

            String htmlContent = templateEngine.process("email/booking-confirmation", context);
            sendHtmlEmailInternal(toEmail, subject, htmlContent);

        } catch (Exception e) {
            System.err.println("❌ Failed to send booking confirmation email: " + e.getMessage());
        }
    }

    // ==================================================
    // 🔹 4. Gửi thông báo đơn đăng ký chủ sở hữu
    // ==================================================
    @Override
    @Async
    public void sendOwnerApplicationNotification(String adminEmail, String applicantName, String applicationId) {
        try {
            String subject = "Đơn đăng ký mới từ chủ sở hữu - Smart Booking";

            Context context = new Context();
            context.setVariable("applicantName", applicantName);
            context.setVariable("applicationId", applicationId);
            context.setVariable("reviewUrl", getFrontendBaseUrl() + "admin/applications/" + applicationId);

            String htmlContent = templateEngine.process("email/owner-application-notification", context);
            sendHtmlEmailInternal(adminEmail, subject, htmlContent);

        } catch (Exception e) {
            System.err.println("❌ Failed to send owner application notification: " + e.getMessage());
        }
    }

    // ==================================================
    // 🔹 5. Gửi kết quả xét duyệt đơn đăng ký
    // ==================================================
    @Override
    @Async
    public void sendApplicationStatusEmail(String toEmail, String fullName, String status, String reason) {
        try {
            String subject = "Kết quả xét duyệt đơn đăng ký - Smart Booking";

            Context context = new Context();
            context.setVariable("username", fullName);
            context.setVariable("status", status);
            context.setVariable("reason", reason);
            context.setVariable("loginUrl", getFrontendBaseUrl() + "login");

            String htmlContent = templateEngine.process("email/application-status", context);
            sendHtmlEmailInternal(toEmail, subject, htmlContent);

        } catch (Exception e) {
            System.err.println("❌ Failed to send application status email: " + e.getMessage());
        }
    }

    // ==================================================
    // ✉️ Private Helper - Gửi Email HTML (ĐÃ SỬA ĐỂ HIỆN LOGO)
    // ==================================================
    private void sendHtmlEmailInternal(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        // --- 👇 PHẦN QUAN TRỌNG: FIX LỖI HIỂN THỊ TRÊN MÁY TÍNH ---
        try {
            // 1. Tìm file ảnh
            String path = "static/images/LogoTriptify.png";
            ClassPathResource logoResource = new ClassPathResource(path);

            if (logoResource.exists()) {
                // 2. Đính kèm VÀ khai báo rõ đây là "image/png"
                // Outlook máy tính bắt buộc phải có tham số thứ 3 này mới chịu hiện ảnh
                helper.addInline("logoImage", logoResource, "image/png");
            } else {
                System.err.println("⚠️ Cảnh báo: Không tìm thấy logo tại: " + path);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi đính kèm logo: " + e.getMessage());
        }
        // -----------------------------------------------------------

        mailSender.send(message);
    }

    // ==================================================
    // 🔄 Async gửi Email với template tùy chọn
    // ==================================================
    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        try {
            String htmlContent = templateEngine.process(templateName, context);
            sendHtmlEmailInternal(to, subject, htmlContent);
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khi gửi email async: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPaymentReminderEmail(String toEmail, String fullName, String bookingId, String totalPrice) {
        try {
            String subject = "Vui lòng thanh toán cho đơn đặt phòng #" + bookingId;

            Context context = new Context();
            context.setVariable("username", fullName);
            context.setVariable("bookingId", bookingId);
            context.setVariable("totalPrice", totalPrice);

            String htmlContent = templateEngine.process("email/booking-pending", context);
            sendHtmlEmailInternal(toEmail, subject, htmlContent);

        } catch (Exception e) {
            System.err.println("❌ Failed to send payment reminder email: " + e.getMessage());
        }
    }

    // 1. Gửi khi khách vừa bấm hủy (Chờ duyệt)
    @Override
    @Async
    public void sendCancellationRequestReceivedEmail(String toEmail, String fullName, String bookingId, BigDecimal totalPrice, BigDecimal penalty, BigDecimal refund) {
        try {
            Context context = new Context();
            context.setVariable("username", fullName);
            context.setVariable("bookingId", bookingId);

            context.setVariable("totalPrice", String.format("%,.0f", totalPrice));
            context.setVariable("penaltyAmount", String.format("%,.0f", penalty));
            context.setVariable("refundAmount", String.format("%,.0f", refund));

            String htmlContent = templateEngine.process("email/cancellation-request", context);
            sendHtmlEmailInternal(toEmail, "TravelMate - Xác nhận yêu cầu hủy phòng #" + bookingId, htmlContent);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail cancellation-request: " + e.getMessage());
        }
    }

    // 2. Gửi khi Admin đã duyệt (Thành công)
    @Override
    @Async
    public void sendCancellationSuccessEmail(String toEmail, String fullName, String bookingId, String refundAmount, String penaltyAmount) {
        try {
            Context context = new Context();
            context.setVariable("username", fullName);
            context.setVariable("bookingId", bookingId);
            context.setVariable("refundAmount", refundAmount);
            context.setVariable("penaltyAmount", penaltyAmount);

            String htmlContent = templateEngine.process("email/cancellation-success", context);
            sendHtmlEmailInternal(toEmail, "TravelMate - Hoàn tất hoàn tiền #" + bookingId, htmlContent);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail cancellation-success: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendCheckinReminderEmail(String toEmail, String fullName, String bookingId, String propertyName, String checkInDate) {
        try {
            String subject = "Nhắc nhở: Bạn có lịch check-in vào ngày mai - Smart Booking";

            Context context = new Context();
            context.setVariable("username", fullName);
            context.setVariable("bookingId", bookingId);
            context.setVariable("propertyName", propertyName);
            context.setVariable("checkInDate", checkInDate);
            // Link xem chi tiết booking
            context.setVariable("bookingUrl", getFrontendBaseUrl() + "customer/bookings");

            // Đảm bảo bạn đã tạo file template checkin-reminder.html
            String htmlContent = templateEngine.process("email/checkin-reminder", context);

            // Gọi hàm gửi email nội bộ
            sendHtmlEmailInternal(toEmail, subject, htmlContent);

        } catch (Exception e) {
            // Log lỗi nhưng không ném exception để tránh làm gián đoạn vòng lặp gửi email cho người khác
            System.err.println("❌ Lỗi gửi email nhắc nhở check-in cho booking " + bookingId + ": " + e.getMessage());
        }
    }

    // Gửi email cảm ơn sau khi Check-out
    @Override
    @Async
    public void sendThankYouEmail(String toEmail, String fullName, String bookingId, String propertyName) {
        try {
            String subject = "Cảm ơn bạn đã lựa chọn " + propertyName + " - Smart Booking";


            String reviewUrl = getFrontendBaseUrl() + "customer/bookings";

            Context context = new Context();
            context.setVariable("username", fullName);
            context.setVariable("propertyName", propertyName);
            context.setVariable("bookingId", bookingId);
            context.setVariable("reviewUrl", reviewUrl);

            // Sử dụng template: src/main/resources/templates/email/checkout-thankyou.html
            String htmlContent = templateEngine.process("email/checkout-thankyou", context);

            sendHtmlEmailInternal(toEmail, subject, htmlContent);

        } catch (Exception e) {
            System.err.println("❌ Failed to send thank you email: " + e.getMessage());
        }
    }

    @Override
    public void sendPropertySuspensionEmail(String to, String ownerName, String propertyName, String reason) {
        Context context = new Context();
        context.setVariable("ownerName", ownerName);
        context.setVariable("propertyName", propertyName);
        context.setVariable("reason", reason);

        // Tạo file template: resources/templates/email/property-suspended.html
        sendHtmlEmail(to, "Thông báo dừng hoạt động cơ sở lưu trú", "email/property-suspended", context);
    }

    @Override
    public void sendRoomSuspensionEmail(String to, String ownerName, String propertyName, String roomName, String reason) {
        Context context = new Context();
        context.setVariable("ownerName", ownerName);
        context.setVariable("propertyName", propertyName);
        context.setVariable("roomName", roomName);
        context.setVariable("reason", reason);

        // Tạo file template: resources/templates/email/room-suspended.html
        sendHtmlEmail(to, "Thông báo dừng hoạt động phòng", "email/room-suspended", context);
    }

    @Override
    public void sendPropertyReactivationEmail(String to, String ownerName, String propertyName) {
        Context context = new Context();
        context.setVariable("ownerName", ownerName);
        context.setVariable("propertyName", propertyName);
        sendHtmlEmail(to, "Cơ sở lưu trú đã hoạt động trở lại", "email/property-reactivated", context);
    }

    @Override
    public void sendRoomReactivationEmail(String to, String ownerName, String propertyName, String roomName) {
        Context context = new Context();
        context.setVariable("ownerName", ownerName);
        context.setVariable("propertyName", propertyName);
        context.setVariable("roomName", roomName);
        sendHtmlEmail(to, "Phòng đã hoạt động trở lại", "email/room-reactivated", context);
    }

    @Override
    @Async
    public void sendAccountLockedEmail(String to, String name, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("reason", reason);

            String htmlContent = templateEngine.process("email/account-locked", context);

            helper.setTo(to);
            helper.setSubject("TravelMate - Thông báo khóa tài khoản");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Lỗi gửi email khóa tài khoản: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendRefundRejectionEmail(String toEmail, String fullName, String bookingId, String rejectionReason) {
        try {
            String subject = "TravelMate - Yêu cầu hoàn tiền #" + bookingId + " bị từ chối";

            Context context = new Context();
            context.setVariable("username", fullName);
            context.setVariable("bookingId", bookingId);
            context.setVariable("reason", rejectionReason);
            // Nếu bạn có trang liên hệ, có thể thêm link này vào template
            context.setVariable("contactUrl", getFrontendBaseUrl() + "contact");

            String htmlContent = templateEngine.process("email/refund-rejected", context); // Cần tạo file template này
            sendHtmlEmailInternal(toEmail, subject, htmlContent);

        } catch (Exception e) {
            System.err.println("❌ Failed to send refund rejection email: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendOtpEmail(String toEmail, String otpCode, com.example.smart_booking_system.enums.OtpType type) {
        try {
            String subject = "Mã OTP xác thực - Smart Booking";
            String templateName = "email/otp-email"; // Default fallback

            switch (type) {
                case REGISTER:
                    templateName = "email/otp-registration";
                    subject = "Mã OTP đăng ký tài khoản - Smart Booking";
                    break;
                case FORGOT_PASSWORD:
                    templateName = "email/otp-forgot-password";
                    subject = "Mã OTP đặt lại mật khẩu - Smart Booking";
                    break;
                case TWO_FACTOR_AUTH:
                    templateName = "email/otp-2fa";
                    subject = "Mã OTP xác thực 2 bước - Smart Booking";
                    break;
                case LOGIN_2FA:
                    templateName = "email/otp-2fa-login";
                    subject = "Mã OTP đăng nhập - Smart Booking";
                    break;
            }

            Context context = new Context();
            context.setVariable("otpCode", otpCode);
            String htmlContent = templateEngine.process(templateName, context);
            sendHtmlEmailInternal(toEmail, subject, htmlContent);
        } catch (Exception e) {
            System.err.println("❌ Failed to send OTP email: " + e.getMessage());
        }
    }
}
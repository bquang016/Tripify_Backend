package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.RefundSubmitDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.PaymentResponseDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.*;
import com.example.smart_booking_system.repository.*;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.PaymentIntentCreateParams;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final RefundRequestRepository refundRepo;
    private final EmailService emailService;
    private final PromotionRepository promotionRepo;
    private final NotificationService notificationService;
    private final EntityManager entityManager;
    private final UserRepository userRepository;


    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    // ==========================================
    // THÊM MỚI: LOGIC STRIPE GIAI ĐOẠN 1
    // ==========================================

    /**
     * Lấy hoặc tạo mới Stripe Customer dựa trên User hiện tại
     */
    public String getOrCreateStripeCustomer(User user) throws Exception {
        // Nếu đã có stripeCustomerId trong DB thì trả về luôn
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isEmpty()) {
            return user.getStripeCustomerId();
        }

        // Nếu chưa có, tạo mới trên hệ thống Stripe
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getFullName())
                .build();

        Customer customer = Customer.create(params);

        // Lưu lại stripeCustomerId vào Database
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);

        return customer.getId();
    }

    /**
     * Tạo SetupIntent để Frontend có thể hiển thị form và lưu thẻ an toàn
     */
    public String createSetupIntent(String customerId) throws Exception {
        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                .setCustomer(customerId)
                .addPaymentMethodType("card")
                .build();

        SetupIntent setupIntent = SetupIntent.create(params);
        // Trả về client_secret để truyền xuống Frontend
        return setupIntent.getClientSecret();
    }

    /**
     * GIAI ĐOẠN 2: Lấy danh sách thẻ đã lưu của một Customer
     */
    public PaymentMethodCollection getSavedCards(String customerId) throws Exception {
        PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build();
        return PaymentMethod.list(params);
    }

    /**
     * GIAI ĐOẠN 2: Khởi tạo thanh toán trừ tiền bằng thẻ đã lưu
     */
    public PaymentIntent createPaymentIntentWithSavedCard(Long bookingId, String customerId, String paymentMethodId, Long amountVnd) throws Exception {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountVnd) // Đơn vị là VND (không có số thập phân)
                .setCurrency("vnd")
                .setCustomer(customerId)
                .setPaymentMethod(paymentMethodId)
                .setOffSession(true) // Cho phép thanh toán ngầm (nếu user không online)
                .setConfirm(true) // Xác nhận thanh toán ngay lập tức
                .putMetadata("booking_id", String.valueOf(bookingId)) // Gắn Booking ID để Webhook nhận diện
                .build();

        return PaymentIntent.create(params);
    }

    // =================================================================
    // 1. SUBMIT PAYMENT (Khách thanh toán thành công -> Chốt đơn)
    // =================================================================
    @Transactional
    public ApiResponse<?> submitPayment(int bookingId, String note, String paymentMethod) {
        // 1. Kiểm tra dữ liệu
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        entityManager.refresh(booking);

        // [DEBUG] In ra log để kiểm tra giá lúc bắt đầu thanh toán
        System.out.println("DEBUG: Payment submitting for Booking " + bookingId + " - Current DB Price: " + booking.getTotalPrice());

        Payment payment = paymentRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment info not found"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return ApiResponse.error("Đơn hàng không ở trạng thái chờ thanh toán.");
        }

        // 2. Xử lý logic khuyến mãi (Promotion Count)
        // Tăng count mã Owner
        if (booking.getPromotionCode() != null) {
            int updatedRows = promotionRepo.incrementUsageCountIfAvailable(booking.getPromotionCode());
            if (updatedRows == 0) throw new RuntimeException("Mã giảm giá Owner '" + booking.getPromotionCode() + "' không khả dụng.");
        }
        // Tăng count mã Admin
        if (booking.getAdminPromotionCode() != null) {
            int updatedRows = promotionRepo.incrementUsageCountIfAvailable(booking.getAdminPromotionCode());
            if (updatedRows == 0) throw new RuntimeException("Mã giảm giá Admin '" + booking.getAdminPromotionCode() + "' không khả dụng.");
        }

        // 3. Cập nhật thông tin thanh toán
        payment.setPaymentMethod(paymentMethod);

        // 🔥 Đảm bảo lấy giá từ Booking (lúc này chắc chắn đúng nhờ saveAndFlush bên trên nếu có)
        payment.setTotalAmount(booking.getTotalPrice());

        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setConfirmedDate(LocalDateTime.now()); // Set thêm ngày confirm nếu cần

        String trxRef = "TRX_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payment.setTransactionReference(trxRef);
        payment.setNote(note);
        paymentRepo.save(payment);

        // 4. Cập nhật trạng thái Booking
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepo.save(booking); // Lúc này save chỉ update status, giá giữ nguyên

        // 5. Gửi Email xác nhận
        sendConfirmationEmail(booking, trxRef);

        // 6. [LOGIC MỚI] Gửi thông báo (Notification)
        try {
            String relatedId = String.valueOf(booking.getBookingId());
            String priceFormatted = String.format("%,.0f", booking.getTotalPrice());

            // --- A. Gửi cho OWNER (Dùng Type: BOOKING_RECEIVED) ---
            User owner = booking.getProperty().getOwner();
            if (owner != null) {
                notificationService.sendNotification(
                        owner.getUserId(), // Lấy String ID
                        "Thanh toán thành công #" + booking.getBookingId(),
                        "Khách hàng " + booking.getCustomerName() + " đã thanh toán " + priceFormatted + " VNĐ. Đơn hàng đã được xác nhận!",
                        NotificationType.BOOKING_RECEIVED, // Dùng type dành cho Owner
                        relatedId
                );
            }

            // --- B. [BỔ SUNG] Gửi cho CUSTOMER (Dùng Type: PAYMENT_SUCCESS) ---
            // Khách hàng cũng cần biết mình đã thanh toán thành công
            User customer = booking.getUser();
            if (customer != null) {
                notificationService.sendNotification(
                        customer.getUserId(),
                        "Thanh toán thành công",
                        "Bạn đã thanh toán thành công " + priceFormatted + " VNĐ cho đơn đặt phòng tại " + booking.getProperty().getPropertyName(),
                        NotificationType.PAYMENT_SUCCESS, // Dùng type dành cho Customer
                        relatedId
                );
            }

        } catch (Exception e) {
            // Log lỗi notification không được làm ảnh hưởng transaction chính
            System.err.println("Lỗi gửi thông báo thanh toán: " + e.getMessage());
        }

        return ApiResponse.success("Thanh toán thành công!", new PaymentResponseDTO(payment, null));
    }

    @Transactional
    public ApiResponse<?> requestRefundByUser(int bookingId, RefundSubmitDTO req) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElseThrow(() -> new RuntimeException("Payment not found"));

        if (booking.getStatus() != BookingStatus.CANCELLED) return ApiResponse.error("Phải Hủy phòng trước khi yêu cầu.");
        if (payment.getPaymentStatus() != PaymentStatus.APPROVED) return ApiResponse.error("Chưa thanh toán thành công.");
        if (refundRepo.existsByBooking(booking)) return ApiResponse.error("Đã có yêu cầu đang chờ xử lý.");

        RefundRequest refund = new RefundRequest();
        refund.setBooking(booking);
        refund.setAmount(booking.getRefundAmount());
        refund.setReason(req.getReason());
        refund.setBankName(req.getBankName());
        refund.setAccountNumber(req.getAccountNumber());
        refund.setAccountHolder(req.getAccountHolder());
        refund.setStatus(RefundRequestStatus.PENDING);
        refund.setRequestDate(LocalDateTime.now());
        refundRepo.save(refund);

        payment.setPaymentStatus(PaymentStatus.REFUND_REQUESTED);
        paymentRepo.save(payment);

        return ApiResponse.success("Gửi yêu cầu thành công", new PaymentResponseDTO(payment, refund));
    }

    // ✅ SỬA: Hàm xử lý hoàn tiền (Admin duyệt)
    @Transactional
    public ApiResponse<?> processRefund(int refundRequestId, boolean isApproved, String adminNote) {
        // 1. Kiểm tra dữ liệu
        RefundRequest refund = refundRepo.findById(refundRequestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn tiền"));

        Booking booking = refund.getBooking();
        Payment payment = paymentRepo.findByBooking_BookingId(booking.getBookingId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (refund.getStatus() != RefundRequestStatus.PENDING) {
            return ApiResponse.error("Yêu cầu này đã được xử lý trước đó.");
        }

        // 2. Cập nhật trạng thái
        if (isApproved) {
            refund.setStatus(RefundRequestStatus.APPROVED);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAmount(refund.getAmount());
        } else {
            refund.setStatus(RefundRequestStatus.REJECTED);
            // Nếu từ chối hoàn tiền, trạng thái thanh toán quay về đã thanh toán (APPROVED)
            payment.setPaymentStatus(PaymentStatus.APPROVED);
        }

        refund.setResolveDate(LocalDateTime.now());
        refund.setAdminNote(adminNote);
        refundRepo.save(refund);
        paymentRepo.save(payment);

        // 3. ✅ GỬI EMAIL & THÔNG BÁO
        try {
            String relatedId = String.valueOf(booking.getBookingId());
            String amountFormatted = String.format("%,.0f", refund.getAmount());

            // --- A. TRƯỜNG HỢP DUYỆT (APPROVED) ---
            if (isApproved) {
                // 1. Gửi Email cho khách (Logic cũ của bạn)
                String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
                String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();

                emailService.sendCancellationSuccessEmail(
                        emailTo,
                        nameTo,
                        relatedId,
                        amountFormatted, // Số tiền hoàn
                        String.format("%,.0f", booking.getPenaltyAmount()) // Phí phạt
                );

                // 2. [FIX] Gửi thông báo cho OWNER
                User owner = booking.getProperty().getOwner();
                if (owner != null) {
                    notificationService.sendNotification(
                            owner.getUserId(), // [SỬA] Lấy String ID
                            "Hoàn tiền Booking #" + booking.getBookingId(),
                            "Admin đã chấp thuận hoàn tiền " + amountFormatted + " VNĐ cho khách hàng. Số dư của bạn sẽ được cập nhật.",
                            NotificationType.BOOKING_CANCELLED_BY_GUEST, // [SỬA] Dùng Type của Owner (Vì hoàn tiền thường đi kèm hủy)
                            relatedId
                    );
                }

                // 3. [MỚI] Gửi thông báo cho CUSTOMER (Quan trọng)
                // Khách cần biết yêu cầu của mình đã được duyệt
                User customer = booking.getUser();
                if (customer != null) {
                    notificationService.sendNotification(
                            customer.getUserId(),
                            "Yêu cầu hoàn tiền được duyệt",
                            "Yêu cầu hoàn tiền " + amountFormatted + " VNĐ cho đơn #" + booking.getBookingId() + " đã được chấp thuận.",
                            NotificationType.REFUND_PROCESSED, // [SỬA] Dùng Type của Customer
                            relatedId
                    );
                }
            }

            // --- B. TRƯỜNG HỢP TỪ CHỐI (REJECTED) ---
            else {
                // Nên báo cho khách biết tại sao bị từ chối
                User customer = booking.getUser();
                if (customer != null) {
                    notificationService.sendNotification(
                            customer.getUserId(),
                            "Yêu cầu hoàn tiền bị từ chối",
                            "Admin đã từ chối hoàn tiền cho đơn #" + booking.getBookingId() + ". Lý do: " + adminNote,
                            NotificationType.GENERAL, // Dùng type chung vì không có Type REJECTED_REFUND
                            relatedId
                    );
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi gửi email/thông báo hoàn tiền: " + e.getMessage());
        }

        return ApiResponse.success(isApproved ? "Đã duyệt hoàn tiền" : "Đã từ chối hoàn tiền", null);
    }

    public List<PaymentResponseDTO> getUserTransactionHistory(String userId) {
        List<Payment> payments = paymentRepo.findByBooking_User_UserIdOrderByPaymentDateDesc(userId);
        return payments.stream().map(p -> new PaymentResponseDTO(p, refundRepo.findByBooking(p.getBooking()).orElse(null))).collect(Collectors.toList());
    }

    public List<PaymentResponseDTO> getAllTransactions() {
        return paymentRepo.findAllByOrderByPaymentDateDesc().stream().map(p -> new PaymentResponseDTO(p, refundRepo.findByBooking(p.getBooking()).orElse(null))).collect(Collectors.toList());
    }

    private void sendConfirmationEmail(Booking booking, String trxRef) {
        try {
            emailService.sendBookingConfirmationEmail(booking.getUser().getEmail(), booking.getUser().getFullName(), String.valueOf(booking.getBookingId()));
        } catch (Exception e) { System.err.println("Error mail: " + e.getMessage()); }
    }
}
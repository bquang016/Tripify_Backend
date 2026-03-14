package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.RefundSubmitDTO;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.BookingService;
import com.example.smart_booking_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.smart_booking_system.dto.request.ChargeRequest;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Value;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final BookingService bookingService;


    // ==========================================
    // THÊM MỚI: API STRIPE GIAI ĐOẠN 1
    // ==========================================

    @PostMapping("/stripe/setup-intent")
    public ResponseEntity<?> createSetupIntent() { // KHÔNG dùng @AuthenticationPrincipal nữa
        try {
            // 1. Lấy thông tin User an toàn từ Security Context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập để thực hiện chức năng này."));
            }

            String userEmail = null;
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                userEmail = ((UserDetails) principal).getUsername();
            } else {
                userEmail = principal.toString();
            }

            // Tìm User thật trong DB
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

            // 2. Lấy Customer ID từ Stripe (tạo mới nếu User chưa có)
            String customerId = paymentService.getOrCreateStripeCustomer(user);

            // 3. Yêu cầu tạo SetupIntent
            String clientSecret = paymentService.createSetupIntent(customerId);

            // 4. Trả về cho Frontend
            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", clientSecret);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể khởi tạo luồng lưu thẻ: " + e.getMessage()));
        }
    }

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    // --- Hàm Helper hỗ trợ lấy User an toàn (Tránh lỗi NullPointerException) ---
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng trong hệ thống."));
    }

    // ==========================================
    // API GIAI ĐOẠN 2: STRIPE
    // ==========================================

    @GetMapping("/stripe/cards")
    public ResponseEntity<?> getSavedCards() {
        try {
            User user = getAuthenticatedUser();
            if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isEmpty()) {
                return ResponseEntity.ok(List.of()); // Trả về list rỗng nếu chưa có thẻ
            }

            var cards = paymentService.getSavedCards(user.getStripeCustomerId());
            // cards.getData() chứa danh sách các đối tượng PaymentMethod của Stripe
            return ResponseEntity.ok(cards.getData());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/stripe/charge")
    public ResponseEntity<?> chargeSavedCard(@RequestBody ChargeRequest request) {
        try {
            User user = getAuthenticatedUser();
            if (user.getStripeCustomerId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Người dùng chưa có phương thức thanh toán."));
            }

            // Gọi Stripe API để thực hiện trừ tiền
            PaymentIntent intent = paymentService.createPaymentIntentWithSavedCard(
                    request.getBookingId(),
                    user.getStripeCustomerId(),
                    request.getPaymentMethodId(),
                    request.getAmount()
            );

            // Nếu ngân hàng yêu cầu xác thực 3D Secure (Mã OTP)
            if ("requires_action".equals(intent.getStatus()) || "requires_confirmation".equals(intent.getStatus())) {
                return ResponseEntity.ok(Map.of(
                        "requiresAction", true,
                        "clientSecret", intent.getClientSecret()
                ));
            }

            // ✅ SỬA LỖI TẠI ĐÂY: Nếu thanh toán thành công ngay lập tức (succeeded)
            if ("succeeded".equals(intent.getStatus())) {
                // 1. Cập nhật trạng thái Booking
                bookingService.confirmBookingPayment(request.getBookingId().intValue());

                // 2. LƯU LẠI PAYMENT INTENT ID VÀO DATABASE ĐỂ SAU NÀY HOÀN TIỀN
                // Giả sử paymentService có hàm lưu lịch sử giao dịch:
                paymentService.saveStripeTransaction(
                        request.getBookingId().intValue(),
                        intent.getId(), // Đây là mã pi_xxxxxx cần thiết cho việc Refund
                        request.getAmount(),
                        "STRIPE_CARD"
                );
            }

            return ResponseEntity.ok(Map.of("success", true, "status", intent.getStatus()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Webhook lắng nghe phản hồi từ Stripe
    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Webhook Signature Error");
        }

        // ✅ SỬA LỖI TẠI ĐÂY: Thay TODO bằng lệnh gọi DB thực tế
        switch (event.getType()) {
            case "payment_intent.succeeded":
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
                String bookingIdStr = paymentIntent.getMetadata().get("booking_id");

                if (bookingIdStr != null) {
                    try {
                        int bookingId = Integer.parseInt(bookingIdStr);
                        // Cập nhật Database
                        bookingService.confirmBookingPayment(bookingId);
                        System.out.println("Webhook: Thanh toán thành công cho Booking: " + bookingId);
                    } catch (Exception ex) {
                        System.err.println("Webhook Error: " + ex.getMessage());
                    }
                }
                break;
            case "payment_intent.payment_failed":
                PaymentIntent failedIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
                String failBookingId = failedIntent.getMetadata().get("booking_id");
                System.out.println("Webhook: Thanh toán thất bại cho Booking: " + failBookingId);
                // Có thể viết thêm hàm bookingService.failBookingPayment(failBookingId) nếu cần
                break;
            default:
                break;
        }
        return ResponseEntity.ok("Received");
    }

    @DeleteMapping("/stripe/cards/{paymentMethodId}")
    public ResponseEntity<?> deleteSavedCard(@PathVariable String paymentMethodId) {
        try {
            User user = getAuthenticatedUser();
            if (user.getStripeCustomerId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Người dùng chưa có phương thức thanh toán."));
            }

            // Gọi service xoá thẻ
            paymentService.deletePaymentMethod(paymentMethodId, user.getStripeCustomerId());

            return ResponseEntity.ok(Map.of("success", true, "message", "Xoá thẻ thành công"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    // 1. Khách thanh toán (Gọi sau khi Gateway trả về success hoặc nút "Thanh toán ngay")
    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<?> submitPayment(
            @PathVariable int bookingId,
            @RequestParam(defaultValue = "Online Banking") String method,
            @RequestParam(required = false) String note
    ) {
        return ResponseEntity.ok(paymentService.submitPayment(bookingId, note, method));
    }

    // 2. Lịch sử giao dịch của User đang login
    @GetMapping("/my-history")
    public ResponseEntity<?> getMyHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ✅ SỬA LẠI ĐOẠN NÀY
        String userId;
        if (auth.getPrincipal() instanceof CustomUserDetails) {
            // Lấy ID thật sự từ token (đã được map vào CustomUserDetails)
            userId = ((CustomUserDetails) auth.getPrincipal()).getUserId();
        } else {
            // Fallback nếu có lỗi (thường ít khi vào đây nếu đã qua filter)
            userId = auth.getName();
        }

        return ResponseEntity.ok(paymentService.getUserTransactionHistory(userId));
    }

    // 3. Gửi yêu cầu hoàn tiền (User)
    @PostMapping("/{bookingId}/refund-request")
    public ResponseEntity<?> requestRefund(@PathVariable int bookingId, @RequestBody RefundSubmitDTO req) {
        return ResponseEntity.ok(paymentService.requestRefundByUser(bookingId, req));
    }

    // 4. Admin xử lý hoàn tiền
    @PutMapping("/refund-process/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> processRefund(
            @PathVariable int requestId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String note
    ) {
        return ResponseEntity.ok(paymentService.processRefund(requestId, approve, note));
    }

    // 5. Admin xem tất cả giao dịch
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAllTransactions() {
        return ResponseEntity.ok(paymentService.getAllTransactions());
    }
}
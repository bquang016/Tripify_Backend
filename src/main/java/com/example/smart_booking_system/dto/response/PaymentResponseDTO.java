package com.example.smart_booking_system.dto.response;

import com.example.smart_booking_system.entity.Payment;
import com.example.smart_booking_system.entity.RefundRequest;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PaymentResponseDTO {
    // Thông tin từ Payment Entity
    private Integer paymentId;
    private int bookingId;
    private String paymentMethod;
    private BigDecimal amount; // Map từ totalAmount của Payment
    private String transactionReference;
    private String status; // PaymentStatus dạng String
    private LocalDateTime paymentDate;
    private String note;

    // Thông tin hoàn tiền (Object con, frontend sẽ check null biến này)
    private RefundInfoDTO refundInfo;

    // Constructor chính để map dữ liệu
    public PaymentResponseDTO(Payment payment, RefundRequest refund) {
        if (payment != null) {
            this.paymentId = payment.getPaymentId();
            this.bookingId = (payment.getBooking() != null) ? payment.getBooking().getBookingId() : 0;
            this.paymentMethod = payment.getPaymentMethod();
            this.amount = payment.getTotalAmount();
            this.transactionReference = payment.getTransactionReference();
            this.status = (payment.getPaymentStatus() != null) ? payment.getPaymentStatus().name() : "UNKNOWN";
            this.paymentDate = payment.getPaymentDate();
            this.note = payment.getNote();
        }

        // Logic map RefundRequest vào RefundInfoDTO
        if (refund != null) {
            this.refundInfo = new RefundInfoDTO();
            this.refundInfo.setId(refund.getId());
            this.refundInfo.setReason(refund.getReason());
            this.refundInfo.setAmount(refund.getAmount());
            this.refundInfo.setRequestDate(refund.getRequestDate());
            this.refundInfo.setResolveDate(refund.getResolveDate());
            // Map Enum sang String an toàn
            this.refundInfo.setStatus((refund.getStatus() != null) ? refund.getStatus().name() : "PENDING");
            this.refundInfo.setAdminNote(refund.getAdminNote());

            // Map thông tin ngân hàng để Admin chuyển tiền
            this.refundInfo.setBankName(refund.getBankName());
            this.refundInfo.setAccountNumber(refund.getAccountNumber());
            this.refundInfo.setAccountHolder(refund.getAccountHolder());
        } else {
            this.refundInfo = null; // Frontend sẽ dựa vào đây để biết chưa có yêu cầu
        }
    }

    // Inner Class cho thông tin hoàn tiền
    @Data
    @NoArgsConstructor
    public static class RefundInfoDTO {
        private Integer id;
        private String reason;
        private BigDecimal amount;
        private LocalDateTime requestDate;
        private LocalDateTime resolveDate;
        private String status;
        private String adminNote;

        // Thông tin ngân hàng
        private String bankName;
        private String accountNumber;
        private String accountHolder;
    }
}
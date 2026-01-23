package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingId", unique = true)
    private Booking booking;

    @Column(length = 100)
    private String transactionReference; // Mã giao dịch VNPay/Momo

    @Column(length = 50)
    private String paymentMethod; // Nullable khi mới tạo

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 512)
    private String paymentEvidenceUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private PaymentStatus paymentStatus;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime paymentDate;

    private LocalDateTime confirmedDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal refundedAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(insertable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // Mặc định là PENDING (Chờ khách trả tiền)
        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
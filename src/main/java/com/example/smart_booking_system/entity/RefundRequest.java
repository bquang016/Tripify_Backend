package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.RefundRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingId", unique = true, nullable = false)
    private Booking booking;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    private String bankName;
    private String accountNumber;
    private String accountHolder;

    private String refundImage;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    private LocalDateTime requestDate;
    private LocalDateTime resolveDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RefundRequestStatus status;

    @PrePersist
    protected void onCreate() {
        requestDate = LocalDateTime.now();
        if (status == null) status = RefundRequestStatus.PENDING;
    }
}
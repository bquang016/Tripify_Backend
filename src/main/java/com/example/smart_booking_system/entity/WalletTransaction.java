package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.TransactionStatus;
import com.example.smart_booking_system.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    // Liên kết đến Booking nếu là doanh thu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // Sẽ liên kết đến WithdrawalRequest ở dưới bằng ID (hoặc Object tuỳ bạn, dùng ID để tránh vòng lặp)
    @Column(name = "withdrawal_request_id")
    private Long withdrawalRequestId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
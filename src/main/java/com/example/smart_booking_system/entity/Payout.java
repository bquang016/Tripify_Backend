package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "payouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private Double totalRevenue; // Tổng tiền khách đã trả (cho các đơn hoàn thành)
    private Double platformFee;  // Phí hoa hồng hệ thống thu (vd: 15%)
    private Double payoutAmount; // Số tiền thực tế chuyển cho Owner (85%)

    @Column(name = "stripe_transfer_id")
    private String stripeTransferId; // Mã giao dịch tr_xxxx của Stripe khi bắn tiền

    private String status; // PENDING (Đang chờ), PROCESSING (Đang chuyển), COMPLETED (Đã nhận), FAILED (Lỗi)

    @Column(name = "period_start")
    private LocalDateTime periodStart; // Doanh thu tính từ ngày

    @Column(name = "period_end")
    private LocalDateTime periodEnd;   // Đến ngày

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
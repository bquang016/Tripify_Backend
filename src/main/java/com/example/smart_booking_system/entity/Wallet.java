package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.PayoutMethod;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    // ==========================================
    // THÔNG TIN RÚT TIỀN (PAYOUT SETTINGS)
    // ==========================================

    // 1. Thông tin chuyển khoản ngân hàng (Bank Transfer)
    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_holder_name")
    private String accountHolderName;

    @Column(name = "account_number")
    private String accountNumber;

    // 2. Thông tin thẻ qua Stripe Connect
    @Column(name = "stripe_account_id", unique = true)
    private String stripeAccountId; // ID tài khoản Stripe Connect của Owner (vd: acct_1032D82eZvKYlo2C)

    @Column(name = "card_last4", length = 4)
    private String cardLast4; // 4 số cuối của thẻ để hiển thị UI

    @Column(name = "card_brand")
    private String cardBrand; // Loại thẻ (Visa, Mastercard...) để hiển thị icon UI

    // 3. Phương thức nhận tiền mặc định
    @Enumerated(EnumType.STRING)
    @Column(name = "default_payout_method")
    @Builder.Default
    private PayoutMethod defaultPayoutMethod = PayoutMethod.NONE;
}
package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_details")
public class PaymentDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String paymentMethod; // e.g., "bank"

    private String bankName;

    private String accountHolderName;

    private String accountNumber;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "provider")
    private String provider;
}

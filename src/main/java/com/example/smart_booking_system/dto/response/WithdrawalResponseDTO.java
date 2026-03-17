package com.example.smart_booking_system.dto.response;

import com.example.smart_booking_system.entity.WithdrawalRequest;
import com.example.smart_booking_system.enums.WithdrawalStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WithdrawalResponseDTO {
    private Long id;
    private BigDecimal amount;
    private WithdrawalStatus status;
    private String stripeTransferId;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thêm thông tin Owner cho Admin dễ nhìn
    private String ownerEmail;
    private String ownerName;

    public WithdrawalResponseDTO(WithdrawalRequest entity) {
        this.id = entity.getId();
        this.amount = entity.getAmount();
        this.status = entity.getStatus();
        this.stripeTransferId = entity.getStripeTransferId();
        this.adminNote = entity.getAdminNote();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();

        if (entity.getWallet() != null && entity.getWallet().getOwner() != null) {
            this.ownerEmail = entity.getWallet().getOwner().getEmail();
            this.ownerName = entity.getWallet().getOwner().getFullName();
        }
    }
}
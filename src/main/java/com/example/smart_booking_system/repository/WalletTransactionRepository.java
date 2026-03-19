package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.WalletTransaction;
import com.example.smart_booking_system.enums.TransactionStatus;
import com.example.smart_booking_system.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    // ✅ Thêm hàm này để Cron Job dùng
    List<WalletTransaction> findByStatusAndType(TransactionStatus status, TransactionType type);
}
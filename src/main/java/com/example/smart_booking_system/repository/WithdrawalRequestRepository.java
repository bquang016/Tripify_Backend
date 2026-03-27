package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.WithdrawalRequest;
import com.example.smart_booking_system.enums.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    List<WithdrawalRequest> findByStatus(WithdrawalStatus status);
}
package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Tìm Wallet dựa trên object User (Owner)
    Optional<Wallet> findByOwner(User owner);

    // Bạn có thể giữ lại hàm cũ nếu các tính năng khác đang dùng đến nó
    Optional<Wallet> findByOwner_UserId(String userId);
}
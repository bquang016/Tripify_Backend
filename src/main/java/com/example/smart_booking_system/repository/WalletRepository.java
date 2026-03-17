package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // Sử dụng dấu "_" để chỉ định rõ cho Spring Data JPA truy cập vào thuộc tính userId của entity owner (User)
    Optional<Wallet> findByOwner_UserId(String userId);
}
package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
    boolean existsByToken(String token);
    long deleteByExpiresAtBefore(java.time.LocalDateTime time);
}
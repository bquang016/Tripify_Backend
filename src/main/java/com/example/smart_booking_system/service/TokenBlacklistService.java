package com.example.smart_booking_system.service;


import com.example.smart_booking_system.entity.BlacklistedToken;
import com.example.smart_booking_system.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final BlacklistedTokenRepository repo;

    public void blacklist(String token, LocalDateTime expiresAt) {
        if (!repo.existsByToken(token)) {
            repo.save(new BlacklistedToken(token, expiresAt, null));
        }
    }

    public boolean isBlacklisted(String token) {
        return repo.existsByToken(token);
    }

    // cái này để xóa token khi hết hạn hiện chưa dùng tới
    public long purgeExpired(LocalDateTime now) {
        return repo.deleteByExpiresAtBefore(now);
    }
}
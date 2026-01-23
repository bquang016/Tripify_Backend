package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.SocialAccount;
import com.example.smart_booking_system.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
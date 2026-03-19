package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {
    List<Payout> findByOwner_UserIdOrderByCreatedAtDesc(String ownerId);
    List<Payout> findByStatus(String status);
}
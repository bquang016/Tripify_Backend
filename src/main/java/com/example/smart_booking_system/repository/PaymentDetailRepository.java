package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.PaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentDetailRepository extends JpaRepository<PaymentDetail, Long> {
    Optional<PaymentDetail> findByUserUserId(String userId);
    List<PaymentDetail> findByUserId(Long userId);
    Optional<PaymentDetail> findByUserIdAndIsDefaultTrue(Long userId);
}

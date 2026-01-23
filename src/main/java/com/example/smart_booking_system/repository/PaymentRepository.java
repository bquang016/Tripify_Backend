package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByBooking_BookingId(int bookingId);
    List<Payment> findByBooking_User_UserIdOrderByPaymentDateDesc(String userId);
    List<Payment> findAllByOrderByPaymentDateDesc();
}
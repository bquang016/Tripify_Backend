package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Integer> {

    // Tìm theo Booking (thay vì PaymentId)
    Optional<RefundRequest> findByBooking_BookingId(Integer bookingId);

    // Tìm theo Object Booking
    Optional<RefundRequest> findByBooking(Booking booking);

    // Kiểm tra xem Booking này đã gửi yêu cầu chưa
    boolean existsByBooking(Booking booking);
}
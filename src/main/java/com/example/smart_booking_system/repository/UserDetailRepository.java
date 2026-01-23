package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
// === SỬA LỖI (Long -> Integer) ĐỂ KHỚP VỚI ENTITY MỚI CỦA BẠN ===
public interface UserDetailRepository extends JpaRepository<UserDetail, Integer> {
    Optional<UserDetail> findByUser(User user);
}
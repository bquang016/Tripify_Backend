package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    // Lấy lịch sử tìm kiếm của 1 user,
    // sắp xếp mới nhất lên đầu
    List<SearchHistory> findByUserUserIdOrderByCreatedAtDesc(String userId);

    // Dùng Pageable để phân trang nếu lịch sử quá nhiều
    Page<SearchHistory> findByUserUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
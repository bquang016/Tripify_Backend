package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Notification;
import com.example.smart_booking_system.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 1. [SỬA] Dùng @Query để map chính xác n.user.userId
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.type IN :types ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndTypeInOrderByCreatedAtDesc(
            @Param("userId") String userId,
            @Param("types") List<NotificationType> types,
            Pageable pageable
    );

    // 2. [SỬA] Dùng @Query để tránh lỗi "No property id found"
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.userId = :userId AND n.isRead = false AND n.type IN :types")
    long countByUserIdAndIsReadFalseAndTypeIn(
            @Param("userId") String userId,
            @Param("types") List<NotificationType> types
    );

    // 3. Đánh dấu tất cả là đã đọc (Giữ nguyên)
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.type IN :types")
    void markAllAsReadByType(@Param("userId") String userId, @Param("types") List<NotificationType> types);

    // 4. Đánh dấu 1 cái (Giữ nguyên)
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user.userId = :userId")
    void markAsRead(@Param("id") Long id, @Param("userId") String userId);
}
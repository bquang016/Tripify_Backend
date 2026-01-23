package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Rating;
import com.example.smart_booking_system.enums.RatingType; // Import Enum
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {

    // 1. Tìm theo Booking ID
    // Trong Entity Rating: field là "bookingId" (kiểu Booking), trong Booking: field là "bookingId" (int)
    @Query("SELECT r FROM Rating r WHERE r.bookingId.bookingId = :bookingId AND r.isHidden = false")
    List<Rating> getRatingByBookingId(@Param("bookingId") int bookingId);

    // 2. Tìm theo User ID
    // Trong Entity Rating: field là "userId" (kiểu User), trong User: field là "userId" (String)
    @Query("SELECT r FROM Rating r WHERE r.userId.userId = :userId AND r.isHidden = false")
    List<Rating> getRatingByUserId(@Param("userId") String userId);

    // 3. Tìm theo Rating Type
    @Query("SELECT r FROM Rating r WHERE r.ratingType = :ratingType AND r.isHidden = false")
    List<Rating> getRatingByRatingType(@Param("ratingType") RatingType ratingType);

    // 4. Lấy các review bị ẩn
    @Query("SELECT r FROM Rating r WHERE r.isHidden = true")
    List<Rating> getRatingByHidden();

    // 5. Lấy review của Property (Giữ nguyên query JPQL cũ của bạn vì nó đã chuẩn)
    @Query("""
        SELECT r FROM Rating r
        WHERE r.bookingId.property.propertyId = :propertyId
          AND r.isHidden = false
          AND r.ratingType != com.example.smart_booking_system.enums.RatingType.VIOLATION
        ORDER BY r.isPinned DESC, r.createdAt DESC
    """)
    List<Rating> getRatingsByProperty(@Param("propertyId") int propertyId);

    // 6. Check tồn tại (Sửa booking -> bookingId cho khớp entity)
    boolean existsByBookingId_BookingId(int bookingId);

    // 7. Đếm review ghim
    int countByBookingId_Property_PropertyIdAndIsPinnedTrue(int propertyId);

    // ========================================================================
    // 🔥 QUERY CHO OWNER DASHBOARD
    // ========================================================================
    @Query("SELECT r FROM Rating r " +
            "WHERE r.bookingId.property.owner.userId = :ownerId " +
            "ORDER BY r.createdAt DESC")
    List<Rating> findRecentReviewsByOwner(@Param("ownerId") String ownerId, Pageable pageable);
}
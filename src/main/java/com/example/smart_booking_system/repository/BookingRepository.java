package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;


public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // existing overlapping check
    @Query("""

            SELECT b FROM Booking b
       WHERE b.room.roomId = :roomId
         AND b.status IN (
             com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
             com.example.smart_booking_system.enums.BookingStatus.PENDING_PAYMENT
         )
         AND NOT (b.checkOutDate <= :checkInDate OR b.checkInDate >= :checkOutDate)
       """)
    List<Booking> findConfirmedOverlappingByRoomId(
            @Param("roomId") int roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    // --- NEW: convenient finder methods ---
    List<Booking> findByUserUserId(String userId);

    List<Booking> findByPropertyPropertyId(int propertyId);

    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime dateTime);

    @Query("""
       SELECT b FROM Booking b
       WHERE b.room.roomId = :roomId
         AND b.checkOutDate >= :today
         AND b.status IN (
             com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
             com.example.smart_booking_system.enums.BookingStatus.PENDING_PAYMENT,
             com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN
         )
       """)
    List<Booking> findFutureBookingsByRoomId(@Param("roomId") int roomId, @Param("today") LocalDate today);


    // Tính tổng tiền các booking đã hoàn thành/xác nhận của user để xét hạng
    @Query("""
        SELECT SUM(b.totalPrice) FROM Booking b
        WHERE b.user.userId = :userId
        AND b.status IN (
            com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
            com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN,
            com.example.smart_booking_system.enums.BookingStatus.COMPLETED
        )
    """)
    BigDecimal calculateTotalSpentByUser(@Param("userId") String userId);

    List<Booking> findByCheckInDateAndStatus(LocalDate checkInDate, BookingStatus status);

    @Query("SELECT SUM(b.totalPrice) FROM Booking b WHERE b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.COMPLETED)")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.createdAt >= :startTime")
    long countNewBookings(@Param("startTime") LocalDateTime startTime);

    // ✅ FIX: ORDER BY FUNCTION('MONTH', b.checkInDate) thay vì ORDER BY month
    @Query("SELECT FUNCTION('MONTH', b.checkInDate) as month, SUM(b.totalPrice) as revenue " +
            "FROM Booking b " +
            "WHERE FUNCTION('YEAR', b.checkInDate) = :year " +
            "AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.COMPLETED) " +
            "GROUP BY FUNCTION('MONTH', b.checkInDate) " +
            "ORDER BY FUNCTION('MONTH', b.checkInDate) ASC")
    List<Object[]> getMonthlyRevenue(@Param("year") int year);

    // ✅ FIX: ORDER BY FUNCTION('MONTH', b.createdAt) thay vì ORDER BY month
    @Query("SELECT FUNCTION('MONTH', b.createdAt) as month, COUNT(b) as count " +
            "FROM Booking b " +
            "WHERE FUNCTION('YEAR', b.createdAt) = :year " +
            "GROUP BY FUNCTION('MONTH', b.createdAt) " +
            "ORDER BY FUNCTION('MONTH', b.createdAt) ASC")
    List<Object[]> getMonthlyBookingCount(@Param("year") int year);

    @Query("SELECT b.property.propertyName, SUM(b.totalPrice) as revenue, COUNT(b) as bookings " +
            "FROM Booking b " +
            "WHERE b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.COMPLETED) " +
            "GROUP BY b.property.propertyId, b.property.propertyName " +
            "ORDER BY revenue DESC")
    List<Object[]> getTopPerformingHotels(Pageable pageable);

    @Query("SELECT b.property.propertyType, SUM(b.totalPrice) " +
            "FROM Booking b " +
            "WHERE b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.COMPLETED) " +
            "GROUP BY b.property.propertyType")
    List<Object[]> getRevenueByPropertyType();

    List<Booking> findTop10ByOrderByCreatedAtDesc();




    // ========================================================================
    // 🔥 OWNER DASHBOARD QUERIES
    // ========================================================================

    // 1. Check-in hôm nay (Của Owner)
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.property.owner.userId = :ownerId " +
            "AND b.checkInDate = :today " +
            "AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN)")
    long countCheckInsByOwner(@Param("ownerId") String ownerId, @Param("today") LocalDate today);

    // 2. Check-out hôm nay (Của Owner)
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.property.owner.userId = :ownerId " +
            "AND b.checkOutDate = :today " +
            "AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN)")
    long countCheckOutsByOwner(@Param("ownerId") String ownerId, @Param("today") LocalDate today);

    // 3. Doanh thu hôm nay (Tính trên booking được TẠO trong ngày)
    @Query("SELECT SUM(b.totalPrice) FROM Booking b " +
            "WHERE b.property.owner.userId = :ownerId " +
            "AND CAST(b.createdAt AS LocalDate) = :today " +
            "AND b.status != com.example.smart_booking_system.enums.BookingStatus.CANCELLED")
    BigDecimal calculateRevenueTodayByOwner(@Param("ownerId") String ownerId, @Param("today") LocalDate today);

    // 4. Tổng doanh thu toàn thời gian
    @Query("SELECT SUM(b.totalPrice) FROM Booking b " +
            "WHERE b.property.owner.userId = :ownerId " +
            "AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.COMPLETED)")
    BigDecimal calculateTotalRevenueByOwner(@Param("ownerId") String ownerId);

    // 5. Booking mới trong 24h
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.property.owner.userId = :ownerId " +
            "AND b.createdAt >= :startTime")
    long countNewBookingsByOwner(@Param("ownerId") String ownerId, @Param("startTime") LocalDateTime startTime);

    // 6. Biểu đồ doanh thu theo tháng
    @Query("SELECT FUNCTION('MONTH', b.checkInDate) as month, SUM(b.totalPrice) as revenue " +
            "FROM Booking b " +
            "WHERE b.property.owner.userId = :ownerId " +
            "AND FUNCTION('YEAR', b.checkInDate) = :year " +
            "AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.COMPLETED) " +
            "GROUP BY FUNCTION('MONTH', b.checkInDate) " +
            "ORDER BY FUNCTION('MONTH', b.checkInDate) ASC")
    List<Object[]> getMonthlyRevenueByOwner(@Param("ownerId") String ownerId, @Param("year") int year);

    // 7. Biểu đồ số lượng booking theo tháng
    @Query("SELECT FUNCTION('MONTH', b.createdAt) as month, COUNT(b) as count " +
            "FROM Booking b " +
            "WHERE b.property.owner.userId = :ownerId " +
            "AND FUNCTION('YEAR', b.createdAt) = :year " +
            "GROUP BY FUNCTION('MONTH', b.createdAt) " +
            "ORDER BY FUNCTION('MONTH', b.createdAt) ASC")
    List<Object[]> getMonthlyBookingCountByOwner(@Param("ownerId") String ownerId, @Param("year") int year);

    // 8. Booking gần đây
    @Query("SELECT b FROM Booking b WHERE b.property.owner.userId = :ownerId ORDER BY b.createdAt DESC LIMIT 10")
    List<Booking> findRecentBookingsByOwner(@Param("ownerId") String ownerId);

    // 9. Cơ cấu doanh thu theo loại hình
    @Query("SELECT b.property.propertyType, SUM(b.totalPrice) " +
            "FROM Booking b " +
            "WHERE b.property.owner.userId = :ownerId " +
            "AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.COMPLETED) " +
            "GROUP BY b.property.propertyType")
    List<Object[]> getRevenueByPropertyTypeByOwner(@Param("ownerId") String ownerId);

    // ==============================
// ADMIN DASHBOARD FILTER QUERIES
// ==============================

    // 1) Tổng doanh thu có lọc
    @Query("""
    SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b
    WHERE b.status IN (
        com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
        com.example.smart_booking_system.enums.BookingStatus.COMPLETED
    )
    AND (:year IS NULL OR FUNCTION('YEAR', b.checkInDate) = :year)
    AND (:month IS NULL OR FUNCTION('MONTH', b.checkInDate) = :month)
    AND (:city IS NULL OR LOWER(b.property.city) LIKE LOWER(CONCAT('%', :city, '%')))
    AND (:ownerId IS NULL OR b.property.owner.userId = :ownerId)
""")
    BigDecimal calculateFilteredRevenue(@Param("year") Integer year,
                                        @Param("month") Integer month,
                                        @Param("city") String city,
                                        @Param("ownerId") String ownerId);

    // 2) Đếm booking mới (theo createdAt) có lọc
    @Query("""
    SELECT COUNT(b) FROM Booking b
    WHERE (:year IS NULL OR FUNCTION('YEAR', b.createdAt) = :year)
    AND (:month IS NULL OR FUNCTION('MONTH', b.createdAt) = :month)
    AND (:city IS NULL OR LOWER(b.property.city) LIKE LOWER(CONCAT('%', :city, '%')))
    AND (:ownerId IS NULL OR b.property.owner.userId = :ownerId)
""")
    long countFilteredBookings(@Param("year") Integer year,
                               @Param("month") Integer month,
                               @Param("city") String city,
                               @Param("ownerId") String ownerId);

    // 3) Doanh thu theo tháng (hiện 12 tháng) - lọc theo year + city + ownerId
    @Query("""
    SELECT FUNCTION('MONTH', b.checkInDate) as month, COALESCE(SUM(b.totalPrice), 0) as revenue
    FROM Booking b
    WHERE b.status IN (
        com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
        com.example.smart_booking_system.enums.BookingStatus.COMPLETED
    )
    AND FUNCTION('YEAR', b.checkInDate) = :year
    AND (:city IS NULL OR LOWER(b.property.city) LIKE LOWER(CONCAT('%', :city, '%')))
    AND (:ownerId IS NULL OR b.property.owner.userId = :ownerId)
    GROUP BY FUNCTION('MONTH', b.checkInDate)
    ORDER BY FUNCTION('MONTH', b.checkInDate) ASC
""")
    List<Object[]> getFilteredMonthlyRevenue(@Param("year") int year,
                                             @Param("city") String city,
                                             @Param("ownerId") String ownerId);

    // 4) Booking theo tháng (12 tháng) - lọc theo year + city + ownerId
    @Query("""
    SELECT FUNCTION('MONTH', b.createdAt) as month, COUNT(b) as count
    FROM Booking b
    WHERE FUNCTION('YEAR', b.createdAt) = :year
    AND (:city IS NULL OR LOWER(b.property.city) LIKE LOWER(CONCAT('%', :city, '%')))
    AND (:ownerId IS NULL OR b.property.owner.userId = :ownerId)
    GROUP BY FUNCTION('MONTH', b.createdAt)
    ORDER BY FUNCTION('MONTH', b.createdAt) ASC
""")
    List<Object[]> getFilteredMonthlyBookingCount(@Param("year") int year,
                                                  @Param("city") String city,
                                                  @Param("ownerId") String ownerId);

    // 5) Doanh thu theo loại hình có lọc
    @Query("""
    SELECT b.property.propertyType, COALESCE(SUM(b.totalPrice), 0)
    FROM Booking b
    WHERE b.status IN (
        com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
        com.example.smart_booking_system.enums.BookingStatus.COMPLETED
    )
    AND (:year IS NULL OR FUNCTION('YEAR', b.checkInDate) = :year)
    AND (:month IS NULL OR FUNCTION('MONTH', b.checkInDate) = :month)
    AND (:city IS NULL OR LOWER(b.property.city) LIKE LOWER(CONCAT('%', :city, '%')))
    AND (:ownerId IS NULL OR b.property.owner.userId = :ownerId)
    GROUP BY b.property.propertyType
""")
    List<Object[]> getFilteredRevenueByPropertyType(@Param("year") Integer year,
                                                    @Param("month") Integer month,
                                                    @Param("city") String city,
                                                    @Param("ownerId") String ownerId);
    @Query("""
    SELECT DISTINCT b.property.city
    FROM Booking b
    WHERE b.property.city IS NOT NULL AND TRIM(b.property.city) <> ''
    ORDER BY b.property.city
""")
    List<String> findAllCitiesForDashboard();
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.room.roomId = :roomId " +
            "AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, " +
            "                 com.example.smart_booking_system.enums.BookingStatus.PENDING_PAYMENT) " +
            "AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)")
    Long countExistingBookings(@Param("roomId") Integer roomId,
                               @Param("checkIn") LocalDate checkIn,
                               @Param("checkOut") LocalDate checkOut);

    // ========================================================================
    // PAYOUT & ACCOUNTING QUERIES (KẾ TOÁN & ĐỐI SOÁT)
    // ========================================================================

    // Lấy danh sách booking đã HOÀN TẤT của Owner trong khoảng thời gian để chia tiền
    @Query("SELECT b FROM Booking b " +
            "WHERE b.property.owner.userId = :ownerId " +
            "AND b.status = com.example.smart_booking_system.enums.BookingStatus.COMPLETED " +
            "AND b.checkOutDate >= :startDate " +
            "AND b.checkOutDate <= :endDate")
    List<Booking> findCompletedBookingsForPayout(
            @Param("ownerId") String ownerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}

package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    // --- CÁC HÀM CHECK TRÙNG (LOGIC MỚI: BỎ QUA MÃ ĐÃ XÓA) ---

    // Check trùng toàn sàn (Admin): Tìm xem có mã nào TRÙNG TÊN và CHƯA XÓA không?
    boolean existsByCodeAndStatusNot(String code, PromotionStatus status);

    // Check trùng Owner: Admin có mã này (chưa xóa) không?
    boolean existsByCodeAndPropertyIsNullAndStatusNot(String code, PromotionStatus status);

    // Check trùng Owner: Property này có mã này (chưa xóa) không?
    boolean existsByCodeAndProperty_PropertyIdAndStatusNot(String code, Integer propertyId, PromotionStatus status);

    // --- HÀM HỖ TRỢ PAYMENT SERVICE (QUAN TRỌNG) ---
    // Vì DB giờ cho phép trùng code (1 xóa, 1 mới), nên tìm theo code có thể ra nhiều kết quả.
    // Ta lấy list, sắp xếp mới nhất lên đầu.
    @Query("SELECT p FROM Promotion p WHERE p.code = :code ORDER BY p.createdAt DESC")
    List<Promotion> findByCodeRaw(@Param("code") String code);

    // Hàm default để PaymentService gọi mà không bị lỗi compile, cũng không bị lỗi runtime
    // Logic: Lấy mã mới nhất tìm được (Ưu tiên mã Active nếu có, hoặc mã Deleted mới nhất để hiện lịch sử)
    default Optional<Promotion> findByCode(String code) {
        List<Promotion> list = findByCodeRaw(code);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<Promotion> findByStatus(PromotionStatus status);

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.code = :code
        AND p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE
        AND p.startDate <= :now
        AND p.endDate >= :now
        AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)
    """)
    List<Promotion> findValidPromotionsList(@Param("code") String code, @Param("now") LocalDateTime now);

    // Wrapper cho Service gọi (Lấy cái đầu tiên tìm thấy)
    default Optional<Promotion> findValidPromotion(String code, LocalDateTime now) {
        List<Promotion> list = findValidPromotionsList(code, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<Promotion> findByStatusAndEndDateBefore(PromotionStatus status, LocalDateTime date);

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE
        AND p.startDate <= :now
        AND p.endDate >= :now
        AND (p.usageLimit IS NULL OR COALESCE(p.usageCount, 0) < p.usageLimit) 
    """)
    List<Promotion> findAvailablePromotions(@Param("now") LocalDateTime now);

    List<Promotion> findByProperty_PropertyId(int propertyId);

    List<Promotion> findByPropertyIsNull();

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE
        AND p.startDate <= :now
        AND p.endDate >= :now
        AND (p.usageLimit IS NULL OR COALESCE(p.usageCount, 0) < p.usageLimit)
        AND (p.property IS NULL OR p.property.propertyId = :propertyId)
    """)
    List<Promotion> findPromotionsForProperty(@Param("propertyId") int propertyId, @Param("now") LocalDateTime now);

    @Query("SELECT p FROM Promotion p WHERE p.property.owner.userId = :userId AND p.status != com.example.smart_booking_system.enums.PromotionStatus.DELETED ORDER BY p.createdAt DESC")
    List<Promotion> findAllByOwnerId(@Param("userId") String userId);

    @Query("SELECT p FROM Promotion p LEFT JOIN FETCH p.property ORDER BY p.promotionId DESC")
    List<Promotion> findAllWithProperty();

    @Modifying
    @Query("UPDATE Promotion p SET p.usageCount = p.usageCount + 1 " +
            "WHERE p.code = :code " +
            "AND p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE " + // Thêm dòng này
            "AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)")
    int incrementUsageCountIfAvailable(@Param("code") String code);

    // 1. Tìm mã của Owner chính xác
    @Query("SELECT p FROM Promotion p WHERE p.code = :code " +
            "AND p.property.propertyId = :propertyId " +
            "AND :now BETWEEN p.startDate AND p.endDate " +
            "AND p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE")
    Optional<Promotion> findValidPromotionForProperty(@Param("code") String code,
                                                      @Param("propertyId") int propertyId,
                                                      @Param("now") LocalDateTime now);

    // 2. Tìm mã của Admin chính xác
    @Query("SELECT p FROM Promotion p WHERE p.code = :code " +
            "AND p.property IS NULL " +
            "AND :now BETWEEN p.startDate AND p.endDate " +
            "AND p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE")
    Optional<Promotion> findValidAdminPromotion(@Param("code") String code,
                                                @Param("now") LocalDateTime now);

}


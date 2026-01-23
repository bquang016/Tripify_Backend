package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.enums.AmenityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // ✅ Thêm import này

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Integer> {

    @Query("""
            SELECT a FROM Amenity a
            WHERE a.isActive = true
            """)
    List<Amenity> findAllActive();

    @Query("""
            SELECT COUNT(a) > 0
            FROM Amenity a
            WHERE LOWER(a.amenityName) = LOWER(:name)
              AND a.amenityType = :type
            """)
    boolean existsByNameAndType(@Param("name") String name,
                                @Param("type") AmenityType type);

    @Query("""
            SELECT COUNT(a) > 0
            FROM Amenity a
            WHERE LOWER(a.amenityName) = LOWER(:name)
              AND a.amenityType = :type
              AND a.amenityId <> :id
            """)
    boolean existsByNameTypeExcept(@Param("name") String name,
                                   @Param("type") AmenityType type,
                                   @Param("id") int id);

    @Query("""
            SELECT a 
            FROM Amenity a 
            WHERE a.isActive = true 
              AND a.amenityType = :type
            """)
    List<Amenity> findByType(@Param("type") AmenityType type);

    // 1. Tìm chính xác theo tên và loại (Dùng để map dữ liệu khi Owner gửi đơn)
    Optional<Amenity> findByAmenityNameAndAmenityType(String amenityName, AmenityType amenityType);

    // 2. Kiểm tra tồn tại chính xác (Dùng để tránh tạo trùng lặp khi seed dữ liệu)
    boolean existsByAmenityNameAndAmenityType(String amenityName, AmenityType amenityType);
}
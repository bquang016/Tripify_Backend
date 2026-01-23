package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.enums.RoomCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    // ✅ SỬA: findByProperty... (Do biến trong Entity tên là property)
    // ✅ SỬA: ...ActiveTrue (Do biến trong Entity tên là active)
    List<Room> findByProperty_PropertyIdAndActiveTrue(Integer propertyId);

    // Hàm cũ (nếu code cũ có dùng), sửa lại tên cho đúng chuẩn
    List<Room> findByProperty_PropertyId(Integer propertyId);

    @Query("""
        SELECT r FROM Room r
        WHERE 
        (:propertyId IS NULL OR r.property.propertyId = :propertyId)
        AND
        (:keyword IS NULL OR (
            LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ))
        AND r.active = true
    """)
    List<Room> searchRooms(
            @Param("propertyId") Integer propertyId,
            @Param("keyword") String keyword
    );

    // Sửa jpql: r.property.propertyId
    @Query("""
           SELECT r
           FROM Room r
           WHERE r.property.propertyId = :propertyId
             AND r.roomCategory = :category
           """)
    Optional<Room> findByPropertyIdAndCategory(@Param("propertyId") int propertyId,
                                               @Param("category") RoomCategory category);

    // Sửa jpql: r.property.propertyId
    @Query("""
        SELECT COUNT(r) > 0 FROM Room r
        WHERE r.property.propertyId = :propertyId
        AND LOWER(r.roomName) = LOWER(:roomName)
        AND r.roomId != :excludeRoomId
    """)
    boolean existsByProperty_PropertyIdAndRoomNameAndIdNot(
            @Param("propertyId") Integer propertyId,
            @Param("roomName") String roomName,
            @Param("excludeRoomId") Integer excludeRoomId
    );

    List<Room> findAllByProperty_PropertyId(Integer propertyId);


}
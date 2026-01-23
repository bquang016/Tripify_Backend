package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.RoomAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomAmenityRepository extends JpaRepository<RoomAmenity, Integer> {

    // ✅ 1. SỬA QUERY: Đổi ra.roomId.roomId -> ra.room.roomId (Vì tên biến trong entity là 'room')
    @Query("""
        SELECT COUNT(ra) > 0 
        FROM RoomAmenity ra
        WHERE ra.room.roomId = :roomId 
          AND ra.amenity.amenityId = :amenityId
          AND ra.active = true
    """)
    boolean existsByRoomAndAmenity(
            @Param("roomId") int roomId,
            @Param("amenityId") int amenityId
    );

    // ✅ 2. THÊM HÀM NÀY ĐỂ FIX LỖI "Cannot find symbol"
    // Hàm này Spring Data JPA tự động generate query dựa trên tên hàm
    Optional<RoomAmenity> findByRoom_RoomIdAndAmenity_AmenityId(int roomId, int amenityId);

    // ✅ 3. Hàm check tồn tại chuẩn JPA (Dùng cho hàm add trong service)
    boolean existsByRoom_RoomIdAndAmenity_AmenityId(int roomId, int amenityId);

    // ✅ 4. Hàm lấy danh sách theo phòng (Chuẩn JPA)
    List<RoomAmenity> findByRoom_RoomId(int roomId);

    // Query cũ nếu bạn muốn giữ (đã sửa lỗi cú pháp)
    @Query("SELECT ra FROM RoomAmenity ra WHERE ra.room.roomId = :roomId AND ra.active = true")
    List<RoomAmenity> findByRoomId(@Param("roomId") int roomId);

    // Soft Delete
    @Modifying
    @Query("UPDATE RoomAmenity ra SET ra.active = false WHERE ra.roomAmenityId = :id")
    void softDelete(@Param("id") int id);
}
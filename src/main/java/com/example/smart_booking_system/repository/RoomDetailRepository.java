package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.entity.RoomAmenity;
import com.example.smart_booking_system.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomDetailRepository extends JpaRepository<Room, Integer> {

    @Query("SELECT r FROM Room r WHERE r.roomId = :roomId")
    Room getRoomDetail(@Param("roomId") int roomId);

    // ✅ ĐÃ SỬA: roomId -> room, amenityId -> amenity
    @Query("""
        SELECT ra
        FROM RoomAmenity ra
        JOIN FETCH ra.amenity a
        WHERE ra.room.roomId = :roomId
    """)
    List<RoomAmenity> getAmenitiesByRoomId(@Param("roomId") int roomId);

    // ✅ ĐÃ SỬA: roomId -> room
    @Query("SELECT ri FROM RoomImage ri WHERE ri.room.roomId = :roomId")
    List<RoomImage> getImagesByRoomId(@Param("roomId") int roomId);
}
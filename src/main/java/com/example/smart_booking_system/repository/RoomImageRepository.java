package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;
@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Integer> {

    @Query("""
        SELECT ri FROM RoomImage ri
        WHERE ri.room.roomId = :roomId
        AND ri.isActive = true
    """)
    List<RoomImage> findActiveImagesByRoomId(int roomId);
    List<RoomImage> findByRoom_RoomId(int roomId);
    @Modifying
    @Query("UPDATE RoomImage r SET r.isCover = false WHERE r.room.roomId = :roomId")
    void resetCoverImageByRoomId(@Param("roomId") int roomId);
}

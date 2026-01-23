package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.RoomAmenityResponseDTO;
import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.entity.RoomAmenity;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.repository.AmenityRepository;
import com.example.smart_booking_system.repository.RoomAmenityRepository;
import com.example.smart_booking_system.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomAmenityService {

    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;

    // Helper map DTO (Hoặc dùng constructor DTO nếu đã tạo)
    private RoomAmenityResponseDTO toDTO(RoomAmenity ra) {
        return new RoomAmenityResponseDTO(
                ra.getRoomAmenityId(),
                ra.getRoom().getRoomId(),          // ✅ SỬA: getRoom()
                ra.getAmenity().getAmenityId(),    // ✅ SỬA: getAmenity()
                ra.getAmenity().getAmenityName(),  // ✅ SỬA: getAmenity()
                ra.isActive()
        );
    }

    // ------------------ ADD ------------------
    public List<RoomAmenityResponseDTO> addMultipleAmenities(int roomId, List<Integer> amenityIds) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.isActive())
            throw new IllegalArgumentException("Room is inactive");

        List<Integer> invalidAmenityIds = new ArrayList<>();

        // 1. VALIDATION PHASE
        for (int amenityId : amenityIds) {
            Amenity amenity = amenityRepository.findById(amenityId).orElse(null);
            if (amenity == null || !amenity.isActive() || amenity.getAmenityType() != AmenityType.ROOM) {
                invalidAmenityIds.add(amenityId);
            }
        }

        if (!invalidAmenityIds.isEmpty()) {
            throw new IllegalArgumentException("Invalid amenityIds: " + invalidAmenityIds);
        }

        // 2. ADD PHASE
        List<RoomAmenityResponseDTO> result = new ArrayList<>();

        for (int amenityId : amenityIds) {
            // Kiểm tra tồn tại: Cần đảm bảo Repository có hàm này
            // existsByRoom_RoomIdAndAmenity_AmenityId là chuẩn JPA
            if (roomAmenityRepository.existsByRoom_RoomIdAndAmenity_AmenityId(roomId, amenityId))
                continue;

            Amenity amenity = amenityRepository.findById(amenityId).get();

            RoomAmenity ra = new RoomAmenity();
            ra.setRoom(room);       // ✅ SỬA: setRoom(room object)
            ra.setAmenity(amenity); // ✅ SỬA: setAmenity(amenity object)
            ra.setActive(true);

            result.add(toDTO(roomAmenityRepository.save(ra)));
        }

        return result;
    }

    // ------------------ READ ------------------
    public List<RoomAmenityResponseDTO> getRoomAmenities(int roomId) {
        // ✅ SỬA: Gọi hàm theo chuẩn JPA (findByRoom_RoomId)
        return roomAmenityRepository.findByRoom_RoomId(roomId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ------------------ UPDATE ------------------
    public RoomAmenityResponseDTO updateRoomAmenityByAmenityId(int roomId, int oldAmenityId, int newAmenityId) {

        // 1. Tìm RoomAmenity cũ
        // ✅ SỬA: findByRoom_RoomIdAndAmenity_AmenityId
        RoomAmenity existing = roomAmenityRepository.findByRoom_RoomIdAndAmenity_AmenityId(roomId, oldAmenityId)
                .orElseThrow(() -> new IllegalArgumentException("This amenity does not belong to this room"));

        // 2. Lấy amenity mới
        Amenity newAmenity = amenityRepository.findById(newAmenityId)
                .orElseThrow(() -> new IllegalArgumentException("New amenity not found"));

        if (newAmenity.getAmenityType() != AmenityType.ROOM) {
            throw new IllegalArgumentException("Amenity type must be ROOM");
        }

        // 3. Kiểm tra trùng
        boolean alreadyAssigned = roomAmenityRepository.existsByRoom_RoomIdAndAmenity_AmenityId(roomId, newAmenityId);

        if (alreadyAssigned) {
            throw new IllegalArgumentException("This room already has this amenity");
        }

        // 4. Update
        existing.setAmenity(newAmenity); // ✅ SỬA: setAmenity
        RoomAmenity saved = roomAmenityRepository.save(existing);

        return toDTO(saved);
    }

    // ------------------ DELETE ------------------
    @Transactional
    public RoomAmenityResponseDTO deleteRoomAmenity(int roomId, int amenityId) {

        // 1. Tìm bản ghi
        RoomAmenity existing = roomAmenityRepository.findByRoom_RoomIdAndAmenity_AmenityId(roomId, amenityId)
                .orElseThrow(() -> new IllegalArgumentException("This amenity does not belong to this room"));

        // 2. Soft Delete (Set active = false)
        // Lưu ý: Nếu repository ko có hàm softDelete, ta set thủ công và save
        existing.setActive(false);
        RoomAmenity saved = roomAmenityRepository.save(existing);

        // Hoặc nếu muốn xóa cứng: roomAmenityRepository.delete(existing);

        return toDTO(saved);
    }
}
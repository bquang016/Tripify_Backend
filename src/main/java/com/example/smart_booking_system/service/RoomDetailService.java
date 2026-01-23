package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.*;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.RoomAmenityRepository;
import com.example.smart_booking_system.repository.RoomImageRepository;
import com.example.smart_booking_system.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomDetailService {

    // ✅ Inject đúng các Repository cần thiết
    private final RoomRepository roomRepository;
    private final RoomAmenityRepository roomAmenityRepository;
    private final RoomImageRepository roomImageRepository;

    @Transactional(readOnly = true)
    public RoomDetailsResponseDTO getRoomDetails(int roomId) {

        // ============================
        // 1. Room (Lấy thông tin phòng)
        // ============================
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        RoomResponseDTO roomDTO = new RoomResponseDTO();
        roomDTO.setRoomId(room.getRoomId());
        roomDTO.setRoomName(room.getRoomName());
        roomDTO.setRoomCategory(room.getRoomCategory());
        roomDTO.setDescription(room.getDescription());
        roomDTO.setCapacity(room.getCapacity());
        roomDTO.setPricePerNight(room.getPricePerNight());
        roomDTO.setRoomStatus(room.getRoomStatus());
        roomDTO.setActive(room.isActive());

        // Lưu ý: Trong Entity Room biến là 'property' (kiểu Property)
        roomDTO.setPropertyId(room.getProperty().getPropertyId());

        // ============================
        // 2. Amenities (Lấy tiện nghi)
        // ============================
        // Sử dụng findByRoom_RoomId (Chuẩn JPA)
        List<RoomAmenity> amenities = roomAmenityRepository.findByRoom_RoomId(roomId);

        List<RoomAmenityResponseDTO> amenityDTOs = amenities.stream()
                .map(RoomAmenityResponseDTO::new) // ✅ Sử dụng Constructor tiện ích vừa tạo
                .collect(Collectors.toList());

        // ============================
        // 3. Images (Lấy hình ảnh)
        // ============================
        // Sử dụng findByRoom_RoomId (Chuẩn JPA) - Cần đảm bảo Repo có hàm này
        List<RoomImage> images = roomImageRepository.findByRoom_RoomId(roomId);

        List<RoomImageResponseDTO> imageDTOs = images.stream().map(img ->
                new RoomImageResponseDTO(
                        img.getRoomImageId(),
                        img.getRoom().getRoomId(),
                        img.getImageUrl(),
                        img.isCover(),
                        img.isActive()
                )
        ).collect(Collectors.toList());

        // ============================
        // 4. Combine output
        // ============================
        RoomDetailsResponseDTO response = new RoomDetailsResponseDTO();
        response.setRoom(roomDTO);
        response.setAmenities(amenityDTOs);
        response.setImages(imageDTOs);

        return response;
    }
}
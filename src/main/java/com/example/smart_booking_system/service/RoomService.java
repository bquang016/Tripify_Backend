package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.request.room.RoomRequestDTO;
import org.springframework.web.multipart.MultipartFile;
import com.example.smart_booking_system.dto.response.PriceForecastDTO;
import java.time.LocalDate;

import java.util.List;

public interface RoomService {


    void suspendRoom(Integer roomId, String reason);

    List<RoomResponseDTO> getRoomsByPropertyId(int propertyId);


    RoomResponseDTO addRoom(RoomRequestDTO dto, List<MultipartFile> images);


    RoomResponseDTO updateRoom(int roomId, RoomRequestDTO dto, List<MultipartFile> newImages);

    void deleteRoom(int roomId);
    RoomResponseDTO getRoomById(int roomId);

    boolean checkRoomNameExists(int propertyId, String roomName, int excludeRoomId);
    List<PriceForecastDTO> getPriceForecast(int roomId, LocalDate startDate, int days);

    void activateRoom(Integer roomId);

    List<RoomResponseDTO> getAllRoomsByPropertyId(Integer propertyId);


}
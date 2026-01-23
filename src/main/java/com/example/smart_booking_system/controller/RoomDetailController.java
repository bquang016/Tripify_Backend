package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.RoomDetailsResponseDTO;
import com.example.smart_booking_system.service.RoomDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/room-details")
@RequiredArgsConstructor
public class RoomDetailController {

    private final RoomDetailService roomDetailService;

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomDetails(@PathVariable int roomId) {
        try {
            RoomDetailsResponseDTO result = roomDetailService.getRoomDetails(roomId);
            return ResponseEntity.ok(result);

        } catch (RuntimeException ex) {
            return ResponseEntity
                    .status(404)
                    .body("Room not found with id: " + roomId);
        }
    }
}

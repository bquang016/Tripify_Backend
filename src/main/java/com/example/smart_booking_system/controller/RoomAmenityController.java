package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.RoomAmenityResponseDTO;
import com.example.smart_booking_system.dto.request.AddMultipleAmenityRequest;
import com.example.smart_booking_system.dto.request.UpdateRoomAmenityRequest;
import com.example.smart_booking_system.service.RoomAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/roomAmenity")
@RequiredArgsConstructor
public class RoomAmenityController {

    private final RoomAmenityService roomAmenityService;

    // ADD
    @PostMapping("/add-multiple")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> addMultipleAmenities(
            @RequestParam int roomId,
            @RequestBody AddMultipleAmenityRequest request
    ) {
        try {
            return ResponseEntity.status(201)
                    .body(roomAmenityService.addMultipleAmenities(roomId, request.getAmenityIds()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET BY ROOM
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoomAmenities(@PathVariable int roomId) {
        try {
            return ResponseEntity.ok(roomAmenityService.getRoomAmenities(roomId));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PutMapping("/update/room/{roomId}/amenity/{oldAmenityId}")
    public ResponseEntity<?> updateAmenity(
            @PathVariable int roomId,
            @PathVariable int oldAmenityId,
            @RequestBody Map<String, Integer> body
    ) {
        try {

            int newAmenityId = body.get("newAmenityId");

            RoomAmenityResponseDTO dto =
                    roomAmenityService.updateRoomAmenityByAmenityId(roomId, oldAmenityId, newAmenityId);

            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/room/{roomId}/amenity/{amenityId}")
    public ResponseEntity<?> deleteAmenity(
            @PathVariable int roomId,
            @PathVariable int amenityId
    ) {
        try {
            RoomAmenityResponseDTO dto =
                    roomAmenityService.deleteRoomAmenity(roomId, amenityId);

            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Internal server error: " + e.getMessage());
        }
    }

}
